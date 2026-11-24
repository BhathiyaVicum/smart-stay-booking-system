import java.util.ArrayList;
import java.util.List;

public class AuthenticationManager {
    // List to store all registered users (guests and admins)
    private final List<User> users = new ArrayList<>();
    // Tracks the currently logged-in user
    private User loggedInUser;

    // Registers a new guest after checking for duplicate NIC and email
    public Guest registerGuest(Guest guest) {
        for (User u : users) {
            if (u instanceof Guest g) {
                // Reject if NIC/Passport is already registered
                if (g.getNic().equalsIgnoreCase(guest.getNic())) {
                    System.out.println("Registration failed: NIC/Passport already registered.");
                    return null;
                }
                // Reject if email is already registered
                if (g.getEmail().equalsIgnoreCase(guest.getEmail())) {
                    System.out.println("Registration failed: email already registered.");
                    return null;
                }
            }
        }
        // Add the guest to the user list
        users.add(guest);
        return guest;
    }

    // Adds an admin to the user list
    public Admin addAdmin(Admin admin) {
        users.add(admin);
        return admin;
    }

    // Authenticates a user based on username and password
    public User login(String username, String password) {
        for (User u : users) {
            if (u.getLoginId().equalsIgnoreCase(username) && u.getPassword().equals(password)) {
                loggedInUser = u;
                return u;
            }
        }
        return null;
    }

    // Clears the currently logged-in user
    public void logout() {
        loggedInUser = null;
    }

    // Returns the currently logged-in user
    public User getLoggedInUser() {
        return loggedInUser;
    }
}