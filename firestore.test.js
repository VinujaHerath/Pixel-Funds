const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_budget_01";
const BOB_UID = "bob_budget_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function validCategoryPayload(uid, overrides = {}) {
  const now = pastDate(5000);
  return {
    userId: uid,
    name: "Dining",
    iconKey: "dining",
    colorHex: 0xffe07a5f,
    monthlyLimit: 450.0,
    sortOrder: 0,
    sample: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validTransactionPayload(uid, categoryId, overrides = {}) {
  const now = pastDate(5000);
  return {
    userId: uid,
    categoryId,
    amount: 54.25,
    merchantOrNote: "Ramen Nagi Dinner",
    dateEpochDay: 20776,
    sample: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated users cannot read or write profiles, categories, or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = pastDate();

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      seeded: true,
      hasSampleData: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("categories").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
});

test("Owner can create and update UserProfile; shadow fields & immutable mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      seeded: true,
      hasSampleData: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(profileRef.get());

  // Valid update: toggling hasSampleData after clearing sample data
  await assertSucceeds(
    profileRef.update({
      hasSampleData: false,
      updatedAt: pastDate(1000),
    })
  );

  // Reject modifying immutable createdAt
  await assertFails(
    profileRef.update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Reject shadow field injection
  await assertFails(
    profileRef.update({
      hasSampleData: false,
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Reject future timestamp
  await assertFails(
    profileRef.update({
      hasSampleData: true,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Owner can CRUD categories with valid schema; invalid bounds & extra keys fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const categoriesCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("categories");

  // Empty collection list succeeds
  await assertSucceeds(categoriesCol.get());

  // Valid create (no @DocumentId 'id' field)
  await assertSucceeds(
    categoriesCol.doc("dining").set(validCategoryPayload(ALICE_UID))
  );

  // Valid inline monthly limit adjustment
  await assertSucceeds(
    categoriesCol.doc("dining").update({
      monthlyLimit: 520.0,
      updatedAt: pastDate(1000),
    })
  );

  // Valid full custom category edit
  await assertSucceeds(
    categoriesCol.doc("dining").update({
      name: "Restaurants & Takeout",
      iconKey: "coffee",
      colorHex: 0xff3d8b68,
      monthlyLimit: 600.0,
      sample: false,
      updatedAt: pastDate(500),
    })
  );

  // Reject negative/zero monthlyLimit
  await assertFails(
    categoriesCol.doc("bad_limit").set(
      validCategoryPayload(ALICE_UID, { monthlyLimit: 0 })
    )
  );

  // Reject '@DocumentId' id field inside payload
  await assertFails(
    categoriesCol.doc("extra_id").set({
      id: "extra_id",
      ...validCategoryPayload(ALICE_UID),
    })
  );

  // Reject oversized name (>40 chars)
  await assertFails(
    categoriesCol.doc("long_name").set(
      validCategoryPayload(ALICE_UID, { name: "A".repeat(41) })
    )
  );

  // Owner can delete category
  await assertSucceeds(categoriesCol.doc("dining").delete());
});

test("Owner can create, list, edit, reassign, and delete transactions with category existence check", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  const categoriesCol = userRef.collection("categories");
  const txCol = userRef.collection("transactions");

  // Empty list query succeeds
  await assertSucceeds(txCol.get());

  // Creating a transaction referencing a missing category fails
  await assertFails(
    txCol.doc("tx_orphan").set(validTransactionPayload(ALICE_UID, "missing_cat"))
  );

  // Seed categories first
  await assertSucceeds(
    categoriesCol.doc("dining").set(validCategoryPayload(ALICE_UID))
  );
  await assertSucceeds(
    categoriesCol
      .doc("groceries")
      .set(validCategoryPayload(ALICE_UID, { name: "Groceries", iconKey: "groceries" }))
  );

  // Create transaction referencing existing category succeeds
  await assertSucceeds(
    txCol.doc("tx_1").set(validTransactionPayload(ALICE_UID, "dining"))
  );

  // Atomic batch seeding category + transaction together succeeds (existsAfter)
  const batch = aliceDb.batch();
  batch.set(
    categoriesCol.doc("coffee"),
    validCategoryPayload(ALICE_UID, { name: "Coffee", iconKey: "coffee" })
  );
  batch.set(
    txCol.doc("tx_batch"),
    validTransactionPayload(ALICE_UID, "coffee", { amount: 6.5, sample: true })
  );
  await assertSucceeds(batch.commit());

  // Edit transaction details succeeds
  await assertSucceeds(
    txCol.doc("tx_1").update({
      categoryId: "groceries",
      amount: 82.4,
      merchantOrNote: "Trader Joe's",
      dateEpochDay: 20775,
      sample: false,
      updatedAt: pastDate(1000),
    })
  );

  // Reassign category action succeeds
  await assertSucceeds(
    txCol.doc("tx_1").update({
      categoryId: "dining",
      updatedAt: pastDate(500),
    })
  );

  // Reject zero or negative amount
  await assertFails(
    txCol.doc("tx_1").update({
      amount: -5.0,
      updatedAt: pastDate(200),
    })
  );

  // Delete succeeds
  await assertSucceeds(txCol.doc("tx_1").delete());
});

test("Cross-user isolation blocks Bob from accessing or mutating Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = pastDate(10000);
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      seeded: true,
      hasSampleData: true,
      createdAt: now,
      updatedAt: now,
    });
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("categories")
      .doc("dining")
      .set(validCategoryPayload(ALICE_UID));
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set(validTransactionPayload(ALICE_UID, "dining"));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceRef = bobDb.collection("users").doc(ALICE_UID);

  await assertFails(aliceRef.get());
  await assertFails(aliceRef.collection("categories").get());
  await assertFails(aliceRef.collection("categories").doc("dining").get());
  await assertFails(
    aliceRef.collection("categories").doc("dining").update({
      monthlyLimit: 1.0,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(aliceRef.collection("transactions").get());
  await assertFails(aliceRef.collection("transactions").doc("tx_1").delete());

  // Bob cannot spoof userId inside his own path
  await assertFails(
    bobDb
      .collection("users")
      .doc(BOB_UID)
      .collection("categories")
      .doc("spoof")
      .set(validCategoryPayload(ALICE_UID))
  );
});
