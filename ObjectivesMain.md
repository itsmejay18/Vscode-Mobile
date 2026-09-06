Build a real standalone Android mobile IDE based on my existing Custom VSCodium project.

This is the first prototype and testing version. Do NOT implement DSSC student monitoring, anti-cheating, kiosk mode, exams, teacher controls, or school-specific functionality yet.

The objective is to prove that we can build a powerful standalone VS Code-like Android APK capable of creating, editing, installing dependencies for, and running real development projects locally on the Android device.

1. PRIMARY TARGET

Build:

Custom VSCode Mobile

Platform:

Android only for now
Native Android application
Kotlin
Gradle
Primary ABI: arm64-v8a
Prefer Android 10+ compatibility
Buildable as a normal standalone APK

Do NOT use:

Flutter
React Native
Electron
remote browser IDE
cloud IDE
external website as the editor
remote VS Code server as the primary editor

The APK itself must contain the IDE.

2. CORE ARCHITECTURE

Use this architecture:

CUSTOM VSCODE MOBILE APK
         │
         ▼
┌─────────────────────────────────┐
│      Native Android / Kotlin    │
│                                 │
│ • Application lifecycle         │
│ • Storage                       │
│ • File access                   │
│ • Permissions                   │
│ • Keyboard handling             │
│ • Native dialogs                │
│ • Menus                         │
│ • Project management            │
│ • Runtime management            │
│ • Package installation          │
└──────────────┬──────────────────┘
               │
        Android WebView
               │
               ▼
┌─────────────────────────────────┐
│       VS Code Web / Monaco      │
│                                 │
│ Explorer │ Editor │ Tabs        │
│ Search   │ Code   │ Problems    │
│ Terminal │ Output │ Preview     │
└──────────────┬──────────────────┘
               │
       Kotlin ↔ JavaScript
             Bridge
               │
               ▼
┌─────────────────────────────────┐
│      Local Runtime Service      │
│                                 │
│ Bash                            │
│ Git                             │
│ PHP                             │
│ Composer                        │
│ Node.js                         │
│ npm                             │
│ Laravel CLI                     │
│ Future runtimes                 │
└─────────────────────────────────┘

Monaco/VS Code Web resources must be packaged locally with the application rather than loaded from a public website/CDN.

It is acceptable to use an internal localhost service such as:

127.0.0.1

when technically necessary for Monaco, previews, PHP/Laravel development servers, terminal communication, or WebSocket communication.

This localhost service must run inside the Android device and must NOT turn the application into a cloud/web IDE.

3. LOCAL LINUX/DEVELOPMENT ENVIRONMENT

Android cannot execute ordinary Windows/Linux desktop development binaries directly.

Therefore implement a rootless local Linux userspace/runtime environment controlled by the Android application.

It must NOT require:

root access
Termux to be separately installed
another terminal application
a PC connection
an external server

The user should install only our APK.

The local runtime layer should provide a Linux-style environment capable of executing development tools.

Architect this cleanly through something similar to:

RuntimeManager
    ├── LinuxEnvironment
    ├── ShellManager
    ├── PackageManager
    ├── ProcessManager
    ├── RuntimeInstaller
    └── RuntimeRegistry

Do not tightly couple the editor UI directly to Linux execution.

4. TERMINAL

Implement a real interactive integrated terminal.

It must support:

pwd
ls
cd
mkdir
rm
mv
cp
cat
clear
echo
chmod
env
git
php
composer
node
npm
npx

Use Bash as the shell.

Do NOT attempt to install Git Bash because Git Bash is Windows-specific.

On Android use:

Bash + Git

Terminal requirements:

real PTY or equivalent interactive terminal
stdin
stdout
stderr
ANSI colors
command history
Ctrl+C
Ctrl+D
arrow keys
Tab completion where possible
copy terminal output
multiple terminal sessions eventually
terminal working directory follows project

Suggested UI:

TERMINAL

~/projects/my-app $

Do not build a fake terminal that merely recognizes hardcoded commands.

Commands must execute against the actual local runtime environment.

5. DEVELOPMENT RUNTIME SETTINGS

Create a Settings section named:

Development Environments

It should show installed and available tools.

Example:

Development Environments

PHP Development
├─ PHP                 Installed 8.x
├─ Composer            Installed
└─ Laravel Installer   Installed

JavaScript Development
├─ Node.js             Installed
├─ npm                 Installed
└─ npx                 Installed

Version Control
├─ Git                  Installed
└─ Bash                 Installed

Each component needs states such as:

Not Installed
Downloading
Installing
Installed
Update Available
Error

Provide actions:

Install
Update
Remove
Repair

Show:

installed version
installation size
download size
installation directory
status
6. FRAMEWORK MANAGER

Add another Settings section:

Frameworks

Initially support:

Web Development

Laravel
├─ PHP
├─ Composer
├─ Node.js
├─ npm
├─ Git
└─ Laravel Installer

The app should understand dependencies.

For example, if the user presses:

Install Laravel Environment

the application determines that Laravel requires:

PHP
Composer
Node.js
npm
Git
Bash

and offers to install everything needed.

Do not make the user manually determine dependencies.

Architect this using framework manifests such as:

FrameworkDefinition
├── id
├── name
├── description
├── icon
├── requiredRuntimes
├── optionalRuntimes
├── installCommands
├── projectTemplates
└── validationCommands

This will allow us later to add:

React
Vue
Next.js
Express
Django
Flask
Spring
Java
C/C++
Python
Android

without rewriting the entire runtime system.

7. LARAVEL IS THE FIRST FULLY SUPPORTED FRAMEWORK

Laravel is the primary framework for version 1.

Make Laravel development actually usable.

Required tools:

Bash
Git
PHP
Composer
Node.js
npm

PHP should include commonly required Laravel extensions where feasible, including equivalents of:

openssl
mbstring
tokenizer
xml
ctype
json
fileinfo
curl
pdo
pdo_sqlite
sqlite3
zip

Use SQLite as the easiest default database for the first Android Laravel implementation.

Design the database layer so more database engines can potentially be supported later.

8. CREATE PROJECT

Add:

File
  New Project

or a Welcome screen:

Create New Project

Show:

Web
├── Laravel
├── PHP
├── HTML/CSS/JavaScript
└── Node.js

Laravel should be the first completed template.

When Laravel is selected show:

Create Laravel Project

Project Name:
[________________]

Location:
/projects/

Laravel Version:
[Latest Stable ▼]

Database:
[SQLite ▼]

Install frontend dependencies
[✓]

Initialize Git repository
[✓]

[Create Project]
9. AUTOMATIC LARAVEL PROJECT CREATION

When the user presses Create Project:

First check:

PHP
Composer
Git
Node.js
npm

If something is missing:

show:

Laravel requires additional development tools.

PHP              Installed
Composer         Installed
Git              Installed
Node.js          Missing
npm              Missing

[Install Requirements]

After installation, automatically continue project creation.

Then execute the real equivalent of:

composer create-project laravel/laravel project-name

or the appropriate supported Laravel installer workflow.

Then:

cd project-name

optionally:

npm install

and:

git init

Verify:

php artisan --version

The complete actual Laravel project must appear in Explorer:

project-name/
├── app/
├── bootstrap/
├── config/
├── database/
├── public/
├── resources/
├── routes/
├── storage/
├── tests/
├── artisan
├── composer.json
├── package.json
└── vite.config.js

Do NOT generate a fake Laravel directory structure.

Composer must install the actual packages.

10. PROJECT CREATION PROGRESS UI

Display real progress:

Creating Laravel Project

✓ Checking PHP
✓ Checking Composer
✓ Checking Git
✓ Creating project directory
✓ Installing Laravel
⟳ Installing Composer dependencies
○ Installing npm packages
○ Initializing Git repository
○ Preparing SQLite
○ Opening project

Show terminal/log output through a collapsible:

View Installation Log

Do not freeze the Android UI during installation.

Use background coroutines/services for long operations.

11. VS CODE-LIKE INTERFACE

Make the application resemble VS Code/VSCodium instead of creating a generic Android text editor.

Layout:

┌──────────────────────────────────────────────┐
│ Custom VSCode Mobile                        │
├────┬───────────────┬────────────────────────┤
│    │ EXPLORER      │ main.blade.php         │
│ 📁 │ project       ├────────────────────────┤
│ 🔎 │ ├─ app        │                        │
│ ⎇  │ ├─ routes     │       MONACO           │
│ ⚙  │ ├─ resources  │       EDITOR           │
│    │ └─ ...        │                        │
│    │               │                        │
├────┴───────────────┴────────────────────────┤
│ PROBLEMS | OUTPUT | TERMINAL                │
│                                            │
│ ~/projects/app $ php artisan               │
└────────────────────────────────────────────┘

Optimize primarily for tablets and landscape orientation while remaining usable on phones.

12. EXPLORER

Implement functional:

create file
create folder
rename
delete
move
copy
duplicate
refresh
open
save
Save As
file icons
folder icons
nested folders
context menus

Files must represent real files in the project directory.

13. EDITOR

Use Monaco or the reusable VS Code Web editor infrastructure.

Support:

tabs
dirty/unsaved indicator
syntax highlighting
line numbers
autocomplete
bracket matching
indentation
code folding
minimap setting
search
replace
undo
redo
multi-cursor where supported
font size
word wrap
theme
autosave

Initial languages:

PHP
Blade
HTML
CSS
JavaScript
TypeScript
JSON
Markdown
Shell
SQL
14. KOTLIN ↔ JAVASCRIPT BRIDGE

Create a clean bridge between WebView and Android.

Do not expose an unrestricted insecure JavaScript interface.

Define explicit APIs such as:

FileBridge
    openFile()
    saveFile()
    createFile()
    createDirectory()
    deleteFile()
    renameFile()

TerminalBridge
    createSession()
    write()
    resize()
    interrupt()
    closeSession()

ProjectBridge
    createProject()
    openProject()
    closeProject()

RuntimeBridge
    listRuntimes()
    installRuntime()
    removeRuntime()
    getRuntimeVersion()

Validate all paths and block directory traversal attacks.

15. RUNNING LARAVEL

Add a Run button.

For Laravel projects the app should be capable of running:

php artisan serve --host=127.0.0.1

Detect the assigned localhost port.

Provide:

Open Preview

Preview opens inside the application through a separate WebView.

Example:

http://127.0.0.1:8000

Again, localhost is allowed because it is running on the same Android device.

This is NOT a remote website dependency.

Provide:

Start Server
Stop Server
Restart Server
Open Preview
16. NPM / VITE

Laravel projects must also support:

npm install
npm run dev
npm run build

The terminal must allow these commands manually.

Where practical, Run Tasks can also provide buttons for them.

Handle the local ports generated by Vite.

17. COMMAND PALETTE

Implement a VS Code-style command palette:

>

Initial commands:

New File
New Folder
Save
Save All
Open Project
New Project
Toggle Terminal
Run Laravel Server
Stop Laravel Server
npm install
npm run dev
Git Initialize Repository
Open Settings
Change Theme
18. SETTINGS

Settings categories:

Editor
Appearance
Terminal
Files
Development Environments
Frameworks
PHP
Composer
Node.js
npm
Git
Laravel
Storage
About

Make runtime/framework settings data-driven rather than hardcoded throughout the UI.

19. OFFLINE / ONLINE BEHAVIOR

The IDE itself must work without internet after required components are installed.

Offline:

✓ open projects
✓ edit files
✓ save files
✓ terminal
✓ PHP
✓ Git local operations
✓ run existing Laravel project
✓ run existing Node packages
✓ preview localhost project

Internet is permitted for intentional package downloads such as:

installing PHP runtime
installing Composer
installing Node
composer install
composer create-project
npm install
git clone
runtime updates

Do not require communication with our own server merely to use the IDE.

20. STORAGE

Maintain an application workspace such as:

CustomVSCode/
├── projects/
├── runtimes/
├── packages/
├── cache/
├── settings/
└── logs/

Respect modern Android scoped-storage rules.

Allow projects to be imported/exported through Android Storage Access Framework where appropriate.

Do not request broad storage permissions unnecessarily.

21. PROCESS MANAGEMENT

Create a real process manager.

It must track things such as:

php artisan serve
npm run dev
composer
npm
git

Each process should have:

PID/internal ID
command
working directory
status
stdout
stderr
start time
stop action

Processes must not accidentally continue forever after their associated project/session is closed unless explicitly intended.

22. DO NOT FAKE FEATURES

This requirement is extremely important.

Do NOT make UI buttons that pretend a command ran.

Do NOT create fake terminal responses.

Do NOT simulate Composer installation.

Do NOT generate a fake Laravel folder structure.

Do NOT simply display an HTML representation of VS Code.

Every implemented feature must connect to the real underlying functionality.

If a functionality cannot yet be completed, label it clearly as unfinished/TODO rather than implementing fake behavior.

23. ERROR HANDLING

Provide understandable errors.

Example:

Laravel project creation failed.

Composer exited with code 1.

[View Log]
[Retry]

Handle:

no internet
interrupted downloads
insufficient storage
corrupted runtime
Composer errors
npm errors
Git errors
permission errors
invalid project names
process crashes
24. ARCHITECTURE

Keep the project modular.

A recommended conceptual structure is:

app/
├── android/
│   ├── activities/
│   ├── services/
│   ├── storage/
│   ├── terminal/
│   ├── runtime/
│   ├── projects/
│   ├── webview/
│   └── bridge/
│
├── editor/
│   ├── vscode/
│   ├── monaco/
│   ├── explorer/
│   ├── terminal-ui/
│   └── workbench/
│
└── runtime-manifests/
    ├── php.json
    ├── composer.json
    ├── node.json
    ├── git.json
    └── laravel.json

Adapt this structure where appropriate for the existing repository instead of blindly duplicating directories.

25. DEVELOPMENT ORDER

Do not try to implement everything simultaneously.

Build incrementally in this exact order:

Stage 1

Create working Android Kotlin application.

Confirm:

./gradlew assembleDebug

produces an APK.

Stage 2

Embed local Monaco/VS Code Web interface.

Stage 3

Implement real file explorer and saving.

Stage 4

Implement Kotlin ↔ editor bridge.

Stage 5

Implement local shell and PTY terminal.

Stage 6

Implement RuntimeManager.

Stage 7

Get Bash and Git working.

Stage 8

Get PHP working.

Verify:

php -v
Stage 9

Get Composer working.

Verify:

composer --version
Stage 10

Get Node/npm working.

Verify:

node --version
npm --version
Stage 11

Create a Laravel project using Composer.

Stage 12

Open Laravel project in editor.

Stage 13

Run:

php artisan serve --host=127.0.0.1
Stage 14

Display Laravel website using in-app Preview.

Stage 15

Run:

npm install
npm run dev

Only after these are stable should additional frameworks/features be implemented.

26. FIRST MAJOR ACCEPTANCE TEST

The milestone is complete only if I can install the APK on an Android device and perform this flow:

Install APK
     ↓
Open Custom VSCode Mobile
     ↓
Settings
     ↓
Development Environments
     ↓
Install Laravel Environment
     ↓
PHP installed
Composer installed
Node installed
npm installed
Git installed
Bash installed
     ↓
New Project
     ↓
Laravel
     ↓
Name: testapp
     ↓
Create
     ↓
composer create-project runs
     ↓
real Laravel files appear
     ↓
open terminal
     ↓
php artisan --version
     ↓
works
     ↓
php artisan serve
     ↓
Open Preview
     ↓
Laravel welcome page appears

That is the primary proof-of-concept.

27. IMPORTANT RULE FOR THE EXISTING REPOSITORY

Inspect the repository before making changes.

Reuse components from our existing Custom VSCodium source where technically possible, especially the web/workbench/editor components.

Do not destroy the existing desktop application.

Create the Android/mobile implementation in a clearly isolated location or build target so we can eventually maintain:

Custom VSCodium Desktop
       +
Custom VSCode Mobile Android

in the same project or related codebase.

Before modifying major existing architecture:

inspect it;
identify reusable components;
determine which desktop/Electron components cannot run on Android;
replace only the Android-incompatible layer;
preserve reusable editor/workbench code.
28. ENGINEERING PRIORITIES

Priority order:

1. Functionality
2. Stability
3. Local execution
4. File safety
5. Terminal reliability
6. Laravel compatibility
7. Performance
8. UI polish

Do not prioritize animations or cosmetic effects over core functionality.

Keep memory use reasonable because this will eventually run on student Android tablets.

29. START NOW

Analyze the current repository first.

Then create an implementation plan based on what actually exists.

After the architecture is understood, begin implementing Stage 1 immediately rather than only writing documentation.

Work through the stages sequentially.

After each stage:

build it;
resolve compilation errors;
test the implementation where possible;
commit/organize the implementation cleanly;
continue to the next dependency.

Do not redesign unrelated desktop functionality.

The first goal is a real installable standalone Android APK capable of eventually creating and running Laravel locally on the device.

One thing I would tell Muse separately

After giving that prompt, if it asks again which platform to use, choose “Type your own answer” and enter:

Native Android using Kotlin + embedded local VS Code Web/Monaco + a rootless local Linux development runtime. Android only. No Flutter, React Native, Electron, remote website, cloud IDE, or external Termux dependency.

The key difference is that we're not making merely:

Android
   ↓
WebView
   ↓
Monaco

We're making:

                       CUSTOM VSCODE MOBILE
                               │
           ┌───────────────────┼───────────────────┐
           ↓                   ↓                   ↓
       Kotlin App        VS Code/Monaco       Linux Runtime
           │                   │                   │
     Android APIs        Code editing         Bash
     File system         Explorer             Git
     Storage             Tabs                 PHP
     Permissions         Search               Composer
     Processes           Problems             Node
           │             Terminal UI          npm
           └───────────────────┬───────────────────┘
                               ↓
                         REAL PROJECTS
                               ↓
                            Laravel

And the framework manager is important. Later, installing something like Python should become as simple as adding another runtime definition instead of rebuilding the APK architecture.

For example, eventually Settings could become:

Frameworks & SDKs
────────────────────────────────

Web Development
 Laravel              ✓ Installed
 React                 Install
 Vue                   Install
 Next.js               Install

Languages
 PHP                   ✓ 8.x
 Node.js               ✓ 22.x
 Python                Install
 Java JDK              Install
 C/C++                 Install

Tools
 Composer              ✓
 npm                   ✓
 Git                   ✓
 Bash                  ✓