public abstract class User {
    // Common fields shared by all user types (guests and admins)
    private String name;
    private String contactNumber;
    private String password;

    // Constructor initializes the common user details
    public User(String name, String contactNumber, String password) {
        this.name = name;
        this.contactNumber = contactNumber;
        this.password = password;
    }

    // Each subclass defines its own login identifier (email for guests, admin ID for admins)
    public abstract String getLoginId();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}