# Contributing
Thank you for considering contributing to our project! Here are some guidelines to help you get started:

### Getting Started

1. **Clone the Repository**
    - Clone the repository to your local machine using:
      ```bash
      git clone https://github.com/backtrace-labs/backtrace-android.git
      ```

2. **Create a Branch**
    - It's good practice to create a new branch for each feature or bugfix, if you have jira ticket put ticket number as branch prefix:
      ```bash
      git checkout -b jira-ticket/your-feature-name
      ```

### Coding Guidelines

1. **Code Formatting**
    - Make sure that your code is properly formatted using the default Android Studio formatter.
        - In Android Studio, you can format your code by selecting `Code` > `Reformat Code` or by using the shortcut `Ctrl + Alt + L` (Windows/Linux) or `Cmd + Option + L` (macOS).

2. **Optimize Imports**
    - Run 'Optimize imports' to remove unused imports and reorder them.
        - In Android Studio, you can optimize imports by selecting `Code` > `Optimize Imports`. You can also enable this option to run it automatically in `Settings` -> `Editor` -> `General` -> `Auto Import`

3. **Write Tests**
    - Ensure that you write tests for the new functionality or changes made. This helps maintain the integrity of the project.

### Commit and Push

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

### Create a Pull Request

1. **Submit a Pull Request**
    - Go to the repository on GitHub and click on the `New Pull Request` button.
    - Ensure your pull request includes a description of the changes made and references any relevant issues or feature requests.

2. **Review Process**
    - One of the project maintainers will review your pull request. Please be responsive to any comments or suggestions made.

### Additional Notes

- Ensure that your code follows the existing code style and structure.
- Keep your branch up to date with the latest changes from the `master` branch to avoid merge conflicts.

