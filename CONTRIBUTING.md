# Contributing to StarClone

We welcome any contribution to StarClone, and there are multiple ways to contribute:

 - [Reporting a bug](#reporting-a-bug)
 - [Developing](#developing)
 - [Submitting a pull request](#submitting-a-pr)
 - [Requesting a new feature](#requesting-a-new-feature)


## Reporting a bug

Before submitting a bug report, please check:

- [search existing issues](https://github.com/kidager/rclone-android/issues?q=is%3Aissue)
- [rclone documentation](https://rclone.org/)
- [rclone forum](https://forum.rclone.org/)

A lot of problems are errors in `rclone.conf`. If you have [Termux](https://github.com/termux/termux-app) installed, you can install rclone with `pkg install rclone`. Then, export your config from StarClone and test it in Termux or on a desktop PC.

If the issue also occurs on PC or in Termux, use the rclone forum to post your problem. If you've found a bug in rclone itself, open an issue in the rclone repository.

When filing a new bug report, answer all the questions in the template. This includes:
 - App version
 - Exact Android version
 - Your device model and manufacturer
 - Steps to reproduce (enable logging in Settings > Logging > Log rclone errors)
 - Your rclone log (redact any confidential information)
 - Your redacted rclone configuration file if relevant


## Developing

You should first make sure you have:

- Go 1.24+ installed and in your PATH
- Java 17+ installed and in your PATH
- Android SDK command-line tools installed OR the NDK version specified in `gradle.properties`

You can then build the app from Android Studio or from CLI:

```sh
# Debug build
./gradlew assembleRsDebug

# Install on connected device
./gradlew app:installRsDebug
```


## Submitting a PR

Here are a few tips on getting your PR merged:

1. Keep your PR small. Small PRs are easier to review, test and merge.
2. Keep your PR focused. It should have a single, specific purpose.
3. Use the style of the existing code base. Use idiomatic code whenever possible.
4. Rebase your branch before creating your PR.


## Requesting a new feature

Browse existing issues for feature requests and join the discussion. Use reactions instead of '+1' comments.

When opening a new feature request, answer all questions in the template:
- Search for existing issues first
- Check if your idea works in rclone on PC/Termux
- Describe the problem you're trying to solve
- Describe your proposed solution
