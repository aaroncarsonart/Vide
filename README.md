# Vide

Vide is the IDE for [Vikari][0]. Specifically it was designed with the purpose of providing syntax-highlighting for the language. So Vide utilizes the Vikari interpreter directly to lex and parse Vikari source files and support a syntax-highlighting strategy that is backed directly by the interpreter's output.

![A screenshot of Vide showing a Vikari demo file.](images/screenshot_01.png)

# Dependencies

- [Java][1]
- [Maven][2]
- [Vikari][0]
- [jpackage][3]
- [sips][4]
- [iconutil][5]

# Usage

Build the program with Maven. Vikari is a dependency of Vide. So Vikari must be downloaded and built in the `.m2` directory first before Vide will build.

```zsh
# Step 1: install and build Vikari.
cd ~/code/java
git clone https://github.com/atonement-crystals/Vikari.git
cd Vikari
mvn install

# Step 2: Install and build Vide.
cd ~/code/java
git clone https://github.com/atonement-crystals/Vide.git
cd Vide
mvn install

# Execute the jar file directly.
java -jar target/Vide-1.0.0.jar
```

### Aliasing

Suggested usage of Vide is to alias the jar command in `.zshrc`.

```zsh
# Your local root directory for Vide goes here.
VIDE_PROJECT_DIR="~/code/java/Vide"

# Fetch the most recent version of the jar build.
alias vide="java -jar `ls -r ${VIDE_PROJECT_DIR}/target/Vide*.jar | head -n1`"
```

After aliasing Vide, the Vikari demo file as pictured in the screenshot above can be opened with the following command:

```zsh
# Assuming Vikari was downloaded here,
cd ~/code/java/Vikari

# open the demo file for editing.
vide demo/statements.dnr
```

# Cross-Platform Use

Vide builds and runs on macOS, Linux, and Windows. macOS is 100% supported. Linux _should_ work, but 100% functionality cannot be guaranteed on all distributions as it has only been tested on ZorinOS. Windows is not fully supported.

### macOS

Vide was developed primarily for macOS. Therefore, the best support is guaranteed for this platform. A script to generate an installer package file is located at [jpackage.sh](jpackage.sh). Running the script generates the following installer package: `target/Vide-1.0.0.pkg`,  This will install Vide as an app on macOS.

### Linux

Vide has been tested on [ZorinOS 16][6]. Any noticeable incompatibilities have been fixed. Use the executable jar process detailed above to run Vide on Linux.

### Windows

There is a known major bug on Windows. The syntax highlighting is not aligned correctly. The author does not have adequate resources to commit to fully fixing the issue at this time.

[0]: https://github.com/atonement-crystals/Vikari
[1]: https://jdk.java.net/21/
[2]: https://maven.apache.org/download.cgi
[3]: https://docs.oracle.com/en/java/javase/14/docs/specs/man/jpackage.html
[4]: https://blog.smittytone.net/2019/10/24/macos-image-manipulation-with-sips/
[5]: https://www.unix.com/man-page/osx/1/iconutil/
[6]: https://zorin.com/os/download/
