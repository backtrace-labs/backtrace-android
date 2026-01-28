# Contributing
Thank you for your interest in contributing to the Backtrace Android SDK. Please review the following guidelines to help you get started.

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/backtrace-labs/backtrace-android.git
   ```
   
2. **Install pre-commit**
   ```bash
   pip install pre-commit
   pre-commit install
   ```

3. **Create a branch** - it's good practice to create a new branch for each feature or bugfix, if you have jira ticket put ticket number as branch prefix:
   ```bash
   git checkout -b jira-ticket/your-feature-name
   ```
      
4. **Update submodules**
   ```bash
   git submodule update --recursive --remote
   git submodule update --init --recursive
   ```
   
## Coding Guidelines

1. **Code Formatting**
    - Make sure that your code is properly formatted using the [**Spotless**](https://github.com/diffplug/spotless) formatter.
        - From the command line, you can format the code by running:
          ```bash
          ./gradlew spotlessApply
          ```
        - In Android Studio, you can also use the **Spotless Gradle** plugin to run formatting directly from the IDE.

2. **Write Tests**
    - Ensure that you write tests for the new functionality or changes made. This helps maintain the integrity of the project.

## Commit and Push

1. **Commit Your Changes**
    - Write clear and concise commit messages. Follow the convention of using the imperative mood in the subject line.
      ```bash
      git commit -m "Add feature X to improve functionality"
      ```

2. **Push to the Repository**
    - Push your changes to the repository:
      ```bash
      git push origin jira-ticket/your-feature-name
      ```

## Create a Pull Request

1. **Submit a Pull Request**
    - Go to the repository on GitHub and click on the `New Pull Request` button.
    - Ensure your pull request includes a description of the changes made and references any relevant issues or feature requests.

2. **Review Process**
    - One of the project maintainers will review your pull request. Please be responsive to any comments or suggestions made.

## Additional Notes

- Ensure that your code follows the existing code style and structure.
- Keep your branch up to date with the latest changes from the `master` branch to avoid merge conflicts.


## Code Formatting

This project uses **[Spotless](https://github.com/diffplug/spotless)** (a code formatting plugin) integrated with **[pre-commit](https://pre-commit.com/)** to ensure consistent code style and automatic formatting before each commit.

### Setup Instructions
   
1. Run Spotless check
This verifies that your code meets the project’s formatting standards.
   ```bash
   ./gradlew spotlessCheck
   ```

2. (Optional) Automatically reformat code
If formatting issues are found, you can automatically fix them with:
   ```bash
   ./gradlew spotlessApply   
   ```

**Notes**
- The pre-commit hook ensures code formatting is validated automatically before commits are created.
- You can manually trigger all pre-commit checks at any time with:
   ```bash
   pre-commit run --all-files
   ```
