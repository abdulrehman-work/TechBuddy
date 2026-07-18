import { readFileSync } from "node:fs";

const layoutPath = "app/src/main/res/layout/activity_main.xml";
const stringsPath = "app/src/main/res/values/strings.xml";
const manifestPath = "app/src/main/AndroidManifest.xml";
const activityPath =
  "app/src/main/java/org/seniorconnect/app/MainActivity.java";

const layout = readFileSync(layoutPath, "utf8");
const strings = readFileSync(stringsPath, "utf8");
const manifest = readFileSync(manifestPath, "utf8");
const activity = readFileSync(activityPath, "utf8");

const expected = [
  ["action_call", "action_call", "CALL"],
  ["action_youtube", "action_youtube", "YOUTUBE"],
  ["action_speak", "action_speak", "SPEAK"],
  ["action_map", "action_map", "MAP"],
];

const buttonBlocks = layout.match(/<Button\b[\s\S]*?\/>/g) ?? [];

if (buttonBlocks.length !== expected.length) {
  throw new Error(
    `Expected exactly ${expected.length} buttons, found ${buttonBlocks.length}.`,
  );
}

for (const [id, stringName, label] of expected) {
  const block = buttonBlocks.find((value) =>
    value.includes(`android:id="@+id/${id}"`),
  );

  if (!block) {
    throw new Error(`Missing button @+id/${id}.`);
  }

  if (!block.includes(`android:text="@string/${stringName}"`)) {
    throw new Error(`Button ${id} must use @string/${stringName}.`);
  }

  const stringPattern = new RegExp(
    `<string\\s+name="${stringName}">${label}<\\/string>`,
  );
  if (!stringPattern.test(strings)) {
    throw new Error(`String ${stringName} must be exactly ${label}.`);
  }
}

// Phase: limited Android integration. INTERNET is allowed only for the
// TV-style YouTube screen; no other permission may appear.
const permissions = manifest.match(/<uses-permission[^>]*android:name="([^"]+)"/g) ?? [];
const allowedPermissions = new Set(["android.permission.INTERNET"]);
for (const entry of permissions) {
  const name = entry.match(/android:name="([^"]+)"/)[1];
  if (!allowedPermissions.has(name)) {
    throw new Error(`Permission ${name} is not allowed in this phase.`);
  }
}

// The home screen may wire exactly one handler: YouTube. Call, Speak, and
// Map must remain behavior-free.
if (layout.includes("android:onClick")) {
  throw new Error("Wire handlers in code, not with android:onClick in the layout.");
}
const handlerCount = (activity.match(/setOnClickListener/g) ?? []).length;
if (handlerCount !== 1 || !activity.includes("R.id.action_youtube")) {
  throw new Error(
    "MainActivity must wire exactly one handler, on action_youtube; Call, Speak, and Map stay behavior-free.",
  );
}
for (const forbidden of ["action_call", "action_speak", "action_map"]) {
  if (activity.includes(`R.id.${forbidden}`)) {
    throw new Error(`Button ${forbidden} must remain behavior-free in this phase.`);
  }
}

// PiP must stay unavailable on the video screen.
if (/android:supportsPictureInPicture\s*=\s*"true"/.test(manifest)) {
  throw new Error("Picture-in-picture must not be enabled.");
}

console.log(
  "Android shell lint OK: 4 buttons, INTERNET-only permissions, YouTube-only handler, no PiP.",
);
