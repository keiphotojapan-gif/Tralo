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

test("Unauthenticated user cannot read or write trips or savedRoutes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/trips`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/trips/trip_1`).set({
      userId: ALICE_UID,
      title: "Kyoto",
      destination: "京都",
      startDate: "2026-12-01",
      endDate: "2026-12-03",
      budgetJpy: 80000,
      spentJpy: 0,
      status: "PLANNING",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/savedRoutes`).get());
});

test("Owner can create, list, and partially update Trip and subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_kyoto`).set({
      userId: ALICE_UID,
      title: "京都・嵐山 Autumn",
      destination: "京都",
      startDate: "2026-12-01",
      endDate: "2026-12-03",
      companions: "2名",
      budgetJpy: 120000,
      spentJpy: 35000,
      status: "UPCOMING",
      coverKey: "kyoto",
      latitude: 35.0116,
      longitude: 135.7681,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/trips`).get());

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_kyoto`).update({
      spentJpy: 42000,
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_kyoto/items/item_1`).set({
      userId: ALICE_UID,
      tripId: "trip_kyoto",
      dayIndex: 1,
      startTime: "08:30",
      endTime: "10:45",
      title: "のぞみ21号 東京 → 京都",
      category: "TRANSIT",
      locationName: "東京駅",
      lineName: "東海道新幹線",
      platform: "16番線",
      carNumber: "7号車",
      seatNumber: "12E",
      costJpy: 14170,
      bookingStatus: "CONFIRMED",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_kyoto/checklist/chk_1`).set({
      userId: ALICE_UID,
      tripId: "trip_kyoto",
      label: "スマートEX QRコード",
      category: "乗車券・書類",
      checked: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_kyoto/checklist/chk_1`).update({
      checked: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/savedRoutes/route_1`).set({
      userId: ALICE_UID,
      origin: "東京",
      destination: "京都",
      summary: "東海道新幹線 のぞみ",
      durationMinutes: 135,
      fareJpy: 14170,
      transfers: 0,
      legsSummary: "東京 → 京都 (東海道新幹線)",
      favorite: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Rejects @DocumentId id field, immutable createdAt mutation, future timestamps, and ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_bad`).set({
      id: "trip_bad",
      userId: ALICE_UID,
      title: "Invalid",
      destination: "東京",
      startDate: "2026-12-01",
      endDate: "2026-12-02",
      budgetJpy: 10000,
      spentJpy: 0,
      status: "PLANNING",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_future`).set({
      userId: ALICE_UID,
      title: "Future",
      destination: "東京",
      startDate: "2026-12-01",
      endDate: "2026-12-02",
      budgetJpy: 10000,
      spentJpy: 0,
      status: "PLANNING",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_valid`).set({
      userId: ALICE_UID,
      title: "Valid Trip",
      destination: "東京",
      startDate: "2026-12-01",
      endDate: "2026-12-02",
      budgetJpy: 10000,
      spentJpy: 0,
      status: "PLANNING",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_valid`).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/trips/trip_valid`).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's trips or routes", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.doc(`users/${ALICE_UID}/trips/trip_alice`).set({
      userId: ALICE_UID,
      title: "Alice Private",
      destination: "金沢",
      startDate: "2026-12-05",
      endDate: "2026-12-07",
      budgetJpy: 90000,
      spentJpy: 0,
      status: "PLANNING",
      createdAt: created,
      updatedAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/trips/trip_alice`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/trips`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/trips/trip_alice`).update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/trips/trip_alice`).delete());
});
