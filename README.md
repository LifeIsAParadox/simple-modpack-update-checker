![icon.png](https://cdn.modrinth.com/data/I70N6bTC/a38a4215bb7e541cd4d4d7aaea7883effd18b9e1.png)

**Simple Modpack Update Checker** is a lightweight mod designed to check if your modpack is up-to-date. This mod
verifies the version of your modpack against a remote source and notifies you if an update is available.

## Configuration

The configuration file is located at `config/simple-modpack-update-checker.txt` and should contain two lines:

1. **Local Version**: The current version of the installed modpack. This can be any string. **Quotation marks are not
   needed**
2. **Identifier**:  Either a URL to a text file containing the latest version (formatted as `version = "String"`,
   eg [your pack.toml from packwiz](https://raw.githubusercontent.com/SkyblockerMod/Skyblocker-modpack/main/packwiz/pack.toml))
   or the Modrinth project ID.

### Examples

**Using a URL**:

```txt
3.3.3
https://raw.githubusercontent.com/SkyblockerMod/Skyblocker-modpack/main/packwiz/pack.toml
```

**Using a Modrinth Project ID**:

```txt
3.3.3
KmiWHzQ4
```

## Usage

When you start Minecraft with this mod installed, it will automatically check for updates based on your configuration
file and notify you if an update is available. If you use a Modrinth project ID, the mod will only consider the latest
stable version, ignoring alpha and beta versions.

![img.png](https://cdn.modrinth.com/data/I70N6bTC/images/1babc5d3fef1b29f8fd1aa49f845edc21b1d7774.png)

