# WealthWhiz – Personal Budgeting App

## Overview
**WealthWhiz** is a powerful and user-friendly personal budgeting app built with **Android Studio (Kotlin)** and **Room Database**. It helps users take control of their financial life by tracking expenses, managing budgets, monitoring savings goals, and fostering healthy spending habits through visual feedback and gamification elements.

## Key Features

### Authentication
- **Welcome Screen**: Clean launch screen with navigation to Register or Login
- **Register**: New user account creation with validation and persistent local storage
- **Login**: Secure username/password authentication
- **Session Management**: Uses SharedPreferences to track logged-in users

### Dashboard
- **User Profile Widget**: Displays username, login streak, and profile card
- **Budget Summary**: Shows visual indicators for budget status (color-coded)
- **Savings Progress**: Percentage of monthly savings goal completed
- **Spending Bar**: Progress bar comparing actual expenses vs. budget
- **Quick Access Buttons**: Add Expense, Set Budget, Manage Categories, etc.

### Expense Management
- **Add Expense**: Attach date, category, amount, notes, and optional image
- **Expense History**: Sort/filter previous expenses by category or date range
- **Receipt Upload**: Optional photo upload saved using local URI
- **Persistent Storage**: Powered by Room Database with DAO

### Budgeting Tools
- **Set Monthly Budget**: Create and manage overall spending limits
- **Per Category Budgets**: Fine-tune budgets for specific expense types
- **Budget Adjustments**: Modify or reset limits for different periods

### Category Management (Part 2 Focus)
- **Manage Categories**: Create, edit, and delete categories
- **Custom Icons & Colors**: Choose icon and background color
- **Categories Linked to Expenses**: Enforced via foreign key constraints
- **In-app Category Editor**: Edit categories directly from the app
- **Category RecyclerView**: Grid layout with user-specific filtering

### User Profile
- **MaterialCard Layout**: Modern and minimalist info cards
- **User Info Display**: Full name, email, and username fetched from database
- **Logout Functionality**: Clears login state and returns to login screen

### Testing
- **Unit Test**: DAO logic tested using Room in-memory database
- **Instrumentation Test**: Ensures app context is correct
- **GitHub Actions**: Automated testing and build verification

### Installation & Setup
- **Clone the repository**
   https://github.com/Asanda-D/WealthWhizApplication.git

### Installation & Setup
- **WealthWhiz Video Presentation**: https://youtu.be/LKY6PpeIpHg
