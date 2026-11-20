const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/expenses`).doc("exp_1").set({
      userId: ALICE_UID,
      title: "Almuerzo",
      amount: 450,
      categoryId: "food",
      categoryName: "Alimentación",
      expenseDateMillis: Date.now(),
      yearMonth: "2026-11",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/budgets`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/reminders`).get());
});

test("Owner can create, update, list, and delete expenses; cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/expenses`).doc("exp_1").set({
      userId: ALICE_UID,
      title: "Mercado agro",
      amount: 1250.5,
      categoryId: "food",
      categoryName: "Alimentación",
      expenseDateMillis: Date.now(),
      yearMonth: "2026-11",
      note: "Viandas y frutas",
      createdAt: created,
      updatedAt: created,
    })
  );

  // List works even on empty or populated user-scoped subcollections
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertSucceeds(bobDb.collection(`users/${BOB_UID}/expenses`).get());

  // Cross-user access denied
  await assertFails(bobDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/expenses`).doc("exp_1").get());

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/expenses`).doc("exp_1").update({
      amount: 1400,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/expenses`).doc("exp_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field & @DocumentId 'id' field rejected
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/expenses`).doc("exp_2").set({
      id: "exp_2",
      userId: ALICE_UID,
      title: "Invalid",
      amount: 100,
      categoryId: "food",
      categoryName: "Alimentación",
      expenseDateMillis: Date.now(),
      yearMonth: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/expenses`).doc("exp_3").set({
      userId: ALICE_UID,
      title: "Future",
      amount: 100,
      categoryId: "food",
      categoryName: "Alimentación",
      expenseDateMillis: Date.now(),
      yearMonth: "2026-11",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/expenses`).doc("exp_1").delete());
});

test("Debts and atomic batch payments work for owner and reject invalid amounts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  const debtRef = aliceDb.collection(`users/${ALICE_UID}/debts`).doc("debt_1");
  await assertSucceeds(
    debtRef.set({
      userId: ALICE_UID,
      direction: "OWED_TO_ME",
      counterparty: "Carlos Pérez",
      concept: "Préstamo reparación",
      totalAmount: 5000,
      paidAmount: 0,
      status: "PENDING",
      dueDateMillis: Date.now() + 86400000,
      yearMonth: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Atomic payment + parent update
  const batch = aliceDb.batch();
  batch.set(debtRef.collection("payments").doc("pay_1"), {
    userId: ALICE_UID,
    amount: 2000,
    paymentDateMillis: Date.now(),
    note: "Primer abono",
    createdAt: new Date(Date.now() - 1000),
  });
  batch.update(debtRef, {
    paidAmount: 2000,
    status: "PENDING",
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertSucceeds(batch.commit());

  // Paid amount exceeding totalAmount must fail
  await assertFails(
    debtRef.update({
      paidAmount: 6000,
      status: "SETTLED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Orphan payment for non-existent debt must fail
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/debts/nonexistent/payments`).doc("pay_x").set({
      userId: ALICE_UID,
      amount: 100,
      paymentDateMillis: Date.now(),
      createdAt: new Date(Date.now() - 500),
    })
  );
});

test("Budgets, reminders, and custom categories enforce schema and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/budgets`).doc("2026-11_food").set({
      userId: ALICE_UID,
      categoryId: "food",
      categoryName: "Alimentación",
      yearMonth: "2026-11",
      limitAmount: 15000,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/budgets`).doc("2026-11_food").update({
      categoryName: "Alimentación",
      limitAmount: 18000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/reminders`).doc("rem_1").set({
      userId: ALICE_UID,
      title: "Revisar gastos de Alimentación",
      reminderType: "EXPENSE_REVIEW",
      targetId: "food",
      targetName: "Alimentación",
      frequency: "WEEKLY",
      scheduledAtMillis: Date.now() + 86400000,
      enabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/reminders`).doc("rem_1").update({
      enabled: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/categories`).doc("cat_custom_1").set({
      userId: ALICE_UID,
      name: "Mascotas",
      iconKey: "pets",
      colorHex: "#059669",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/budgets`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/reminders`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/categories`).get());
});
