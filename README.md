# VSCode setup (Eclipse-style project)

This repo is not Maven/Gradle. It uses an Eclipse/JDT layout with sources under `Chess/src`.
Follow the steps below so VSCode (Java extension) treats it as a proper Java project.

## Prereqs
- JDK 21 installed.
- VSCode extension: "Extension Pack for Java" (redhat.java).

## Files to add/edit

1) `.vscode/settings.json`
Set the source root and output folder.

```
{
  "java.project.sourcePaths": [
    "Chess/src"
  ],
  "java.project.outputPath": "bin",
  "java.project.referencedLibraries": [
    "Chess/engines/*.jar"
  ],
  "java.debug.settings.vmArgs": "--add-modules=jdk.incubator.vector",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
      "default": true
    }
  ]
}
```

2) `.project`
Defines the Eclipse project so JDT LS imports it correctly.

```
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>ptp25-mi05-chess</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
	<filteredResources>
		<filter>
			<id>1755487182293</id>
			<name></name>
			<type>30</type>
			<matcher>
				<id>org.eclipse.core.resources.regexFilterMatcher</id>
				<arguments>node_modules|\.git|__CREATED_BY_JAVA_LANGUAGE_SERVER__</arguments>
			</matcher>
		</filter>
	</filteredResources>
</projectDescription>
```

3) `.classpath`
Declares the source/output folders and JRE container.

```
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
	<classpathentry kind="src" path="Chess/src"/>
	<classpathentry kind="lib" path="Chess/engines/Serendipity.jar"/>
	<classpathentry kind="con" path="org.eclipse.jdt.junit.JUNIT_CONTAINER/5"/>
	<classpathentry kind="output" path="bin"/>
</classpath>
```

## Commands to apply the changes (from repo root)

```
mkdir -p .vscode
cat > .vscode/settings.json <<'EOF'
{
  "java.project.sourcePaths": [
    "Chess/src"
  ],
  "java.project.outputPath": "bin",
  "java.project.referencedLibraries": [
    "Chess/engines/*.jar"
  ],
  "java.debug.settings.vmArgs": "--add-modules=jdk.incubator.vector",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
      "default": true
    }
  ]
}
EOF

cat > .project <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>ptp25-mi05-chess</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
	<filteredResources>
		<filter>
			<id>1755487182293</id>
			<name></name>
			<type>30</type>
			<matcher>
				<id>org.eclipse.core.resources.regexFilterMatcher</id>
				<arguments>node_modules|\.git|__CREATED_BY_JAVA_LANGUAGE_SERVER__</arguments>
			</matcher>
		</filter>
	</filteredResources>
</projectDescription>
EOF

cat > .classpath <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
	<classpathentry kind="src" path="Chess/src"/>
	<classpathentry kind="lib" path="Chess/engines/Serendipity.jar"/>
	<classpathentry kind="con" path="org.eclipse.jdt.junit.JUNIT_CONTAINER/5"/>
	<classpathentry kind="output" path="bin"/>
</classpath>
EOF
```

## VSCode step to re-import the project

After adding the files, reload the Java workspace:
- VSCode Command Palette: `Java: Clean Java Language Server Workspace`
- Then reopen the workspace if prompted.

## Run in VSCode

Open `Chess/src/com/jeremyzay/zaychess/App.java` and click the Run button.

## Optional: compile/run from terminal

If you want to verify outside VSCode:

```
find Chess/src -name "*.java" > /tmp/sources.txt
javac -d bin -cp "Chess/engines/Serendipity.jar" @/tmp/sources.txt
rsync -a Chess/src/com/jeremyzay/zaychess/view/assets bin/com/jeremyzay/zaychess/view/
java --add-modules=jdk.incubator.vector -cp "bin:Chess/engines/Serendipity.jar" com.jeremyzay.zaychess.App
```
