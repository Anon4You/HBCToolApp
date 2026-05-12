<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0a0a0a&height=180&section=header&text=HBCToolApp&fontSize=90&fontColor=00ff9d&fontStyle=bold&animation=fadeIn&fontAlignY=20&desc=Hermes%20Bytecode%20on%20Android&descSize=30&descColor=cbd5e1" width="100%" />

[![Latest Release](https://img.shields.io/github/v/release/Anon4You/HBCToolApp?style=for-the-badge&label=RELEASE&color=00ff9d&labelColor=111111)](https://github.com/Anon4You/HBCToolApp/releases)
[![License: MIT](https://img.shields.io/badge/LICENSE-MIT-00ff9d?style=for-the-badge&labelColor=111111)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/ANDROID-8.0%2B-00ff9d?style=for-the-badge&labelColor=111111)]()
[![arm64-v8a](https://img.shields.io/badge/ARCH-arm64--v8a-00ff9d?style=for-the-badge&labelColor=111111)]()
[![Kotlin](https://img.shields.io/badge/LANG-Kotlin-ff006e?style=for-the-badge&labelColor=111111)]()
[![Chaquopy](https://img.shields.io/badge/PYTHON-Chaquopy-7c3aed?style=for-the-badge&labelColor=111111)]()

<br/>

<a href="https://github.com/Anon4You/HBCToolApp/releases">
  <img src="https://img.shields.io/badge/⬇️_Download_Now-00ff9d?style=for-the-badge&labelColor=111111&logo=github&logoColor=00ff9d" alt="Download" width="200"/>
</a>

<br/>

<i>Disassemble & assemble <code>.bundle</code> files right from your pocket.</i>

</div>

---

## ✨ Capabilities

<table align="center" width="100%">
  <tr>
    <td width="50%" valign="top">

### 🔓 Disassemble
Convert `.bundle` files into readable `.hasm` assembly + JSON metadata with a single tap.

### 🔧 Assemble
Rebuild working `.bundle` binaries from `.hasm` and JSON sources — fully on-devicee

### 📦 Import / Export
Seamlessly handle disassembly folders via ZIP archives. Pull in, push out, zero friction.

### 🧹 Clean Environment
One-tap wipe of all internal `input/` and `output/` temporary files.

### 📋 Live Logging
Real-time timestamped console output with one-tap copy-to-clipboard.

### 🎨 Modern UI
Edge-to-edge design with dynamic dark/light theming. Feels native, looks sharp.

  </tr>
</table>

---

## 📱 Screenshots

<div align="center">

<table>
  <tr>
    <td><img src="img/screenshot1.jpg" alt="Screenshot 1" width="280"/></td>
    <td><img src="img/screenshot2.jpg" alt="Screenshot 2" width="280"/></td>
  </tr>
</table>

</div>

---

## 🚀 Usage

<table>
  <tr>
    <td width="36" align="center" valign="top">
      <img src="https://img.shields.io/badge/1-00ff9d?style=flat-square&labelColor=111111" alt="1"/>
    </td>
    <td valign="top">
      <b>Select</b> — Tap <i>Select File</i> and choose a <code>.bundle</code>, <code>.hasm</code>, or <code>.zip</code> from your device.
    </td>
  </tr>
  <tr>
    <td width="36" align="center" valign="top">
      <img src="https://img.shields.io/badge/2-00ff9d?style=flat-square&labelColor=111111" alt="2"/>
    </td>
    <td valign="top">
      <b>Disassemble</b> — Load a <code>.bundle</code> and tap <i>Disassemble</i> to extract the <code>.hasm</code> and JSON files.
    </td>
  </tr>
  <tr>
    <td width="36" align="center" valign="top">
      <img src="https://img.shields.io/badge/3-00ff9d?style=flat-square&labelColor=111111" alt="3"/>
    </td>
    <td valign="top">
      <b>Assemble</b> — Load a folder (via ZIP import or picker) containing <code>metadata.json</code>, <code>string.json</code>, and <code>instruction.hasm</code>, then tap <i>Assemble</i>.
    </td>
  </tr>
  <tr>
    <td width="36" align="center" valign="top">
      <img src="https://img.shields.io/badge/4-00ff9d?style=flat-square&labelColor=111111" alt="4"/>
    </td>
    <td valign="top">
      <b>Export</b> — Use <i>Export ZIP</i> for disassembly outputs, or <i>Export Bundle</i> for the assembled <code>.bundle</code>.
    </td>
  </tr>
  <tr>
    <td width="36" align="center" valign="top">
      <img src="https://img.shields.io/badge/5-00ff9d?style=flat-square&labelColor=111111" alt="5"/>
    </td>
    <td valign="top">
      <b>Clean</b> — Tap to instantly delete all files stored in the app's internal directories.
    </td>
  </tr>
</table>

---

## ⚙️ Requirements

> [!IMPORTANT]
> **Built & tested with Termux on Android.**
>
> | Requirement | Detail |
> |:--|:--|
> | **OS** | Android 8.0+ (API 26+) |
> | **Architecture** | 64-bit ARM (`arm64-v8a`) only |
> | **Why no 32-bit?** | Python 3.13 dropped support for 32-bit ARM (`armeabi-v7a`) |

---

## 🙏 Credits & License

<div align="center">

<br/>

**Original [HBC-Tool](https://github.com/Kirlif/HBC-Tool) by [Kirlif](https://github.com/Kirlif)**
&nbsp;•&nbsp;
**Python via [Chaquopy](https://chaquo.com/chaquopy/)**
&nbsp;•&nbsp;
**Built with Kotlin & ❤️**

<br/>

[![License: MIT](https://img.shields.io/badge/LICENSE-MIT-00ff9d?style=for-the-badge&labelColor=111111)](LICENSE)

</div>
