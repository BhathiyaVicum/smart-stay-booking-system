public class Guest extends User {
    private String nic; // NIC/Passport number of the guest
    private String email; // Email address of the guest, also used as the login ID

    public Guest(String name, String contactNumber,
                 String nic, String email, String password) {
        super(name, contactNumber, password);
        this.nic = nic;
        this.email = email;
    }

    // Returns the email as the login identifier
    @Override
    public String getLoginId() { return email; }

    public String getNic() { return nic; }

    public void setNic(String nic) { this.nic = nic; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }
}
