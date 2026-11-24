public class Admin extends User {
    // Unique identifier used as the login ID for the admin
    private String adminId;

    public Admin(String adminId, String name, String contactNumber, String password) {
        super(name, contactNumber, password);
        this.adminId = adminId;
    }

    // Returns the admin ID as the login identifier
    @Override
    public String getLoginId() {
        return adminId;
    }

    public String getAdminId() { return adminId; }

    public void setAdminId(String adminId) { this.adminId = adminId; }
}
