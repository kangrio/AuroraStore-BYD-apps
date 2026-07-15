# AuroraStore-BYD-apps

Automation repository for building and publishing prebuilt APKs patched with **Morphe** for **BYD Android infotainment systems**.

## What this repository does

This repository automatically:

* Checks for the latest Morphe patch bundle
* Applies the required patches
* Generates prebuilt APKs
* Publishes the APKs as GitHub Releases

## Output

Each GitHub Release contains:

* `patch_version`
* Release notes
* Build artifacts

## Automation

The build process is fully automated using GitHub Actions.

A new release is created whenever a newer Morphe patch bundle becomes available.

## Repository Structure

```text
apps/       Original APKs
out/        Generated patched APKs (temporary)
patch/      Downloaded CLI and patch bundle (temporary)
.github/    GitHub Actions workflows
```
