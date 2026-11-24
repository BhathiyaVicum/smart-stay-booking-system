import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SmartStaySystem {
    // Core managers that handle authentication, rooms, invoices, and bookings
    private final AuthenticationManager authenticationManager;
    private final RoomManager roomManager;
    private final InvoiceManager invoiceManager;
    private final BookingManager bookingManager;
    // Scanner for reading user input from the console
    private final Scanner sc = new Scanner(System.in);

    // Constructor initializes all managers and seeds the system with
    // a default admin account and a set of starter rooms
    public SmartStaySystem() {
        authenticationManager = new AuthenticationManager();
        roomManager           = new RoomManager();
        invoiceManager        = new InvoiceManager();
        bookingManager        = new BookingManager(roomManager);

        // Default admin account for first-time login
        authenticationManager.addAdmin(new Admin("admin1", "admin", "0000000000", "admin123"));

        // Pre-populated rooms covering all package types
        roomManager.addRoom(new Room("R101", "Double", "AVAILABLE", new Standard(),    3));
        roomManager.addRoom(new Room("R102", "Queen",  "AVAILABLE", new Deluxe(),      4));
        roomManager.addRoom(new Room("R103", "King",   "AVAILABLE", new Suite(),       5));
        roomManager.addRoom(new Room("R104", "King",   "AVAILABLE", new FamilySuite(), 6));
        roomManager.addRoom(new Room("R105", "Single", "AVAILABLE", new Standard(),    3));
        roomManager.addRoom(new Room("R106", "King",   "AVAILABLE", new Deluxe(),      4));
        roomManager.addRoom(new Room("R107", "Queen",  "AVAILABLE", new Suite(),       5));
    }

    // Application entry point
    public static void main(String[] args) {
        new SmartStaySystem().start();
    }

    // Runs the main loop, showing the top-level menu until the user exits
    public void start() {
        System.out.println("===== Welcome to SmartStay =====");
        boolean running = true;
        while (running) {
            showMainMenu();
            switch (readInt("Choice")) {
                case 1 -> handleGuestRegister();
                case 2 -> handleGuestLogin();
                case 3 -> handleAdminLogin();
                case 0 -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println("Goodbye.");
    }

    // Displays the main menu options
    public void showMainMenu() {
        System.out.println("\n1. Register (Guest)");
        System.out.println("2. Login (Guest)");
        System.out.println("3. Login (Admin)");
        System.out.println("0. Exit");
    }

   // ===========================
    // GUEST
   // ===========================

    // Handles new guest registration by collecting and validating input
    private void handleGuestRegister() {
        System.out.println("\n--- Guest Registration ---");
        String name    = requireNonBlank("Full name");
        String contact = requireContact("Contact number");
        String nic     = requireNonBlank("NIC / Passport");
        String email   = requireEmail("Email");
        String pass    = requireNonBlank("Password");
        Guest g = authenticationManager.registerGuest(
                new Guest(name, contact, nic, email, pass));
        if (g != null) System.out.println("Registered successfully. You may now log in.");
    }

    // Handles guest login and routes to the guest menu on success
    private void handleGuestLogin() {
        System.out.println("\n--- Guest Login ---");
        User u = authenticationManager.login(readLine("Email"), readLine("Password"));
        if (!(u instanceof Guest g)) { System.out.println("Invalid credentials.");
            return;
        } else {
            System.out.println("Login Successful!");
        }
        guestMenu(g);
    }

    // Displays the guest menu and dispatches to the chosen action
    private void guestMenu(Guest guest) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Guest Menu (" + guest.getName() + ") ---");
            System.out.println("1. Search available rooms (by type & date)");
            System.out.println("2. View reservations by check-in date");
            System.out.println("3. Cancel reservation");
            System.out.println("4. Modify reservation");
            System.out.println("5. View invoice for a booking");
            System.out.println("6. My booking history");
            System.out.println("0. Logout");
            switch (readInt("Choice")) {
                case 1 -> browseRoomsAndMaybeBook(guest);
                case 2 -> viewReservationsByCheckInDate(guest);
                case 3 -> cancelReservation(guest);
                case 4 -> modifyReservation(guest);
                case 5 -> viewInvoice(guest);
                case 6 -> showMyBookings(guest);
                case 0 -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    // Lets the guest filter rooms by type and dates, then optionally book one
    private void browseRoomsAndMaybeBook(Guest guest) {
        System.out.println("\nFilter by type:");
        System.out.println("  1. Standard");
        System.out.println("  2. Deluxe");
        System.out.println("  3. Suite");
        System.out.println("  4. Family Suite");
        System.out.println("  0. Any");
        String typeFilter;
        switch (readInt("Choice")) {
            case 1 -> typeFilter = "Standard";
            case 2 -> typeFilter = "Deluxe";
            case 3 -> typeFilter = "Suite";
            case 4 -> typeFilter = "Family Suite";
            default -> typeFilter = null;
        }

        LocalDate in = readDate("Check-in  (YYYY-MM-DD)");
        LocalDate out;
        // Keep asking until check-out is after check-in
        while (true) {
            out = readDate("Check-out (YYYY-MM-DD)");
            if (out.isAfter(in)) break;
            System.out.println("Check-out must be after check-in.");
        }

        // Enforce the minimum advance booking window
        if (in.isBefore(LocalDate.now().plusDays(Booking.MIN_ADVANCE_DAYS))) {
            System.out.println("Check-in must be at least " + Booking.MIN_ADVANCE_DAYS
                    + " days from today.");
            return;
        }

        // Search rooms available for the requested dates
        List<Room> rooms = bookingManager.searchAvailableRoomsForDates(typeFilter, in, out);
        if (rooms.isEmpty()) {
            System.out.println("No available rooms match.");
            return;
        }
        printRoomTableForGuest(rooms, in, out);

        // Allow the guest to pick a room or return
        System.out.print("\nSelect a room ID to book (or press Enter to return): ");
        String pick = sc.nextLine().trim();
        if (pick.isEmpty()) return;
        Room chosen = rooms.stream()
                .filter(r -> r.getRoomId().equalsIgnoreCase(pick))
                .findFirst().orElse(null);
        if (chosen == null) {
            System.out.println("Room '" + pick + "' is not in the list above.");
            return;
        }
        makeReservation(guest, chosen, in, out);
    }

    // Creates a reservation for the guest, using preset room/dates when provided
    private void makeReservation(Guest guest, Room presetRoom,
                                 LocalDate presetIn, LocalDate presetOut) {
        System.out.println("\n--- Make Reservation ---");

        LocalDate in, out;
        // Use preset dates if provided, otherwise collect and validate new ones
        if (presetIn != null && presetOut != null) {
            in = presetIn;
            out = presetOut;
            System.out.println("Check-in  : " + in);
            System.out.println("Check-out : " + out);
        } else {
            while (true) {
                in  = readDate("Check-in  (YYYY-MM-DD)");
                out = readDate("Check-out (YYYY-MM-DD)");
                if (!out.isAfter(in)) {
                    System.out.println("Check-out must be AFTER check-in.");
                    continue;
                }
                if (in.isBefore(LocalDate.now().plusDays(Booking.MIN_ADVANCE_DAYS))) {
                    System.out.println("Check-in must be at least "
                            + Booking.MIN_ADVANCE_DAYS + " days from today.");
                    continue;
                }
                break;
            }
        }

        Room room = presetRoom;
        // If no preset room, let the guest pick from available rooms
        if (room == null) {
            List<Room> rooms = bookingManager.searchAvailableRoomsForDates(null, in, out);
            if (rooms.isEmpty()) {
                System.out.println("No rooms available for " + in + " to " + out + ".");
                return;
            }
            printRoomTableForGuest(rooms, in, out);
            String roomId = readLine("Room ID from the list above");
            room = roomManager.findById(roomId);
            if (room == null) {
                System.out.println("No such room.");
                return;
            }
            if (!rooms.contains(room)) {
                System.out.println("Room " + roomId
                        + " is not available for those dates.");
                return;
            }
        } else {
            System.out.println("Room: " + room.getRoomId()
                    + " (" + room.getRoomPackage().getPackageTypeName()
                    + ", LKR " + room.getRoomPackage().getDailyRate() + "/night)");
        }

        // Collect and validate the number of guests
        int guests;
        while (true) {
            guests = readInt("Number of guests");
            if (guests <= 0) { System.out.println("Must be positive."); continue; }
            if (guests > room.getMaxOccupancy()) {
                System.out.println("Room " + room.getRoomId()
                        + " supports at most " + room.getMaxOccupancy() + " guests.");
                continue;
            }
            break;
        }

        // Show a summary and an estimated invoice before confirming
        long nights = ChronoUnit.DAYS.between(in, out);
        System.out.println("\n--- Booking Summary ---");
        System.out.println("Room      : " + room.getRoomId()
                + " (" + room.getRoomPackage().getPackageTypeName() + ")");
        System.out.println("Check-in  : " + in);
        System.out.println("Check-out : " + out);
        System.out.println("Nights    : " + nights);
        System.out.println("Guests    : " + guests);
        System.out.printf ("Advance   : LKR %.2f  (refundable, returned at checkout)%n", Booking.ADVANCE_AMOUNT);

        String id = "B" + System.currentTimeMillis();
        Booking b = new Booking(id, in, out, guests, "CONFIRMED", guest, room);
        Invoice preview = new Invoice(b);
        preview.calculateBill();
        System.out.println("\n--- Estimated Invoice Preview ---");
        preview.generateDetailedInvoice();

        // Confirm before charging the advance
        if (!confirm("Proceed and charge advance?")) {
            System.out.println("Reservation aborted.");
            return;
        }

        if (!bookingManager.makeReservation(b)) return;
        System.out.println("\nReservation confirmed: " + b.getBookingId());
    }

    // Shows the guest's reservations on a given check-in date
    private void viewReservationsByCheckInDate(Guest guest) {
        LocalDate d = readDate("Check-in date (YYYY-MM-DD)");
        List<Booking> list = bookingManager.viewReservationsByDate(d).stream()
                .filter(b -> b.getGuest().getEmail().equalsIgnoreCase(guest.getEmail()))
                .collect(Collectors.toList());
        if (list.isEmpty()) { System.out.println("No reservations on that date."); return; }
        list.forEach(this::printBooking);
    }

    // Cancels a guest's reservation after ownership and eligibility checks
    private void cancelReservation(Guest guest) {
        Booking b = bookingManager.searchReservationById(readLine("Booking ID"));
        if (b == null) { System.out.println("Not found."); return; }
        // Ensure the booking belongs to the logged-in guest
        if (!b.getGuest().getEmail().equalsIgnoreCase(guest.getEmail())) {
            System.out.println("That booking does not belong to you."); return;
        }
        printBooking(b);
        if (!b.isEligibleForCancellation()) {
            System.out.println("Cannot cancel within 24 hours of check-in."); return;
        }
        if (!confirm("Are you sure you want to cancel this booking?")) {
            System.out.println("Cancellation aborted."); return;
        }
        bookingManager.cancelReservation(b.getBookingId());
    }

    // Modifies a guest's reservation (dates, guests, or both)
    private void modifyReservation(Guest guest) {
        Booking b = bookingManager.searchReservationById(readLine("Booking ID"));
        if (b == null) { System.out.println("Not found."); return; }
        if (!b.getGuest().getEmail().equalsIgnoreCase(guest.getEmail())) {
            System.out.println("That booking does not belong to you."); return;
        }
        printBooking(b);
        if (!b.isEligibleForModification()) {
            System.out.println("Cannot modify within 24 hours of check-in."); return;
        }

        // Ask which fields to change
        System.out.println("\nWhat would you like to change?");
        System.out.println("  1. Dates only");
        System.out.println("  2. Number of guests only");
        System.out.println("  3. Both");
        System.out.println("  0. Cancel");
        int choice = readInt("Choice");
        if (choice == 0) { System.out.println("Modification cancelled."); return; }

        // Default to existing values
        LocalDate in  = b.getCheckInDate();
        LocalDate out = b.getCheckOutDate();
        int guests    = b.getNumberOfGuests();

        // Collect new dates if requested
        if (choice == 1 || choice == 3) {
            while (true) {
                in  = readDate("New check-in  (YYYY-MM-DD)");
                out = readDate("New check-out (YYYY-MM-DD)");
                if (!out.isAfter(in)) {
                    System.out.println("Check-out must be AFTER check-in.");
                    continue;
                }
                if (in.isBefore(LocalDate.now().plusDays(Booking.MIN_ADVANCE_DAYS))) {
                    System.out.println("New check-in must be at least "
                            + Booking.MIN_ADVANCE_DAYS + " days from today.");
                    continue;
                }
                break;
            }
        }

        // Collect new guest count if requested
        if (choice == 2 || choice == 3) {
            while (true) {
                guests = readInt("New number of guests");
                if (guests <= 0) { System.out.println("Must be positive."); continue; }
                if (guests > b.getRoom().getMaxOccupancy()) {
                    System.out.println("Room " + b.getRoom().getRoomId()
                            + " supports at most " + b.getRoom().getMaxOccupancy() + " guests.");
                    continue;
                }
                break;
            }
        }

        // Pre-check room availability only if the dates actually changed
        boolean datesChanged = !in.equals(b.getCheckInDate()) || !out.equals(b.getCheckOutDate());
        if (datesChanged && !bookingManager.isRoomAvailableForDatesExcluding(b.getRoom(), in, out, b)) {
            System.out.println("Cannot modify: another booking on room "
                    + b.getRoom().getRoomId() + " overlaps those dates.");
            return;
        }

        // Preview invoice with the proposed changes
        Booking tempBooking = new Booking(
                b.getBookingId(), in, out, guests,
                b.getBookingStatus(), b.getGuest(), b.getRoom());
        Invoice preview = new Invoice(tempBooking);
        preview.calculateBill();

        System.out.println("\n--- Updated Invoice Preview ---");
        preview.generateDetailedInvoice();

        if (!confirm("Apply these changes?")) {
            System.out.println("Modification aborted.");
            return;
        }

        bookingManager.modifyReservation(b, in, out, guests);
    }

    // Displays the invoice for one of the guest's bookings
    private void viewInvoice(Guest guest) {
        Booking b = bookingManager.searchReservationById(readLine("Booking ID"));
        if (b == null) { System.out.println("Not found."); return; }
        if (!b.getGuest().getEmail().equalsIgnoreCase(guest.getEmail())) {
            System.out.println("That booking does not belong to you."); return;
        }
        invoiceManager.generateDetailedInvoice(invoiceManager.calculateInvoice(b));
    }

    // Shows all bookings made by the logged-in guest
    private void showMyBookings(Guest guest) {
        List<Booking> list = bookingManager.getBookingsForGuest(guest);
        if (list.isEmpty()) { System.out.println("You have no bookings."); return; }
        System.out.println("\n--- My Bookings (" + list.size() + ") ---");
        list.forEach(this::printBooking);
    }

   // ===========================
    // ADMIN
   // ===========================

    // Handles admin login and routes to the admin menu on success
    private void handleAdminLogin() {
        System.out.println("\n--- Admin Login ---");
        User u = authenticationManager.login(readLine("Admin ID"), readLine("Password"));
        if (!(u instanceof Admin)) { System.out.println("Invalid credentials.");
            return;
        } else {
            System.out.println("Login Successful!");
        }
        adminMenu();
    }

    // Displays the admin menu and dispatches to the chosen action
    private void adminMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Add room");
            System.out.println("2. Update room");
            System.out.println("3. Remove room");
            System.out.println("4. Change room availability");
            System.out.println("5. View rooms & bookings");
            System.out.println("6. Search reservations by guest name");
            System.out.println("7. View reservations by check-in date");
            System.out.println("8. All bookings");
            System.out.println("0. Logout");
            switch (readInt("Choice")) {
                case 1 -> addRoom();
                case 2 -> updateRoom();
                case 3 -> removeRoom();
                case 4 -> changeRoomStatus();
                case 5 -> viewRoomsAndBookings();
                case 6 -> searchReservationsByName();
                case 7 -> viewReservationsByDate();
                case 8 -> showAllBookings();
                case 0 -> back = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    // Adds a new room with an auto-generated ID and admin-selected details
    private void addRoom() {
        String id = roomManager.generateNextRoomId();
        System.out.println("Auto-assigned room ID: " + id);
        String bed = pickBedType();
        RoomPackage pkg = pickPackage();
        if (pkg == null) return;

        // Max occupancy = package base capacity + allowed extra guests
        int extraSlots = readExtraSlots(pkg);
        int maxOcc = pkg.getBaseCapacity() + extraSlots;

        roomManager.addRoom(new Room(id, bed, "AVAILABLE", pkg, maxOcc));
        System.out.println("Room " + id + " added ("
                + pkg.getPackageTypeName() + ", " + bed
                + ", " + pkg.getBaseCapacity() + " included + " + extraSlots + " extra = "
                + maxOcc + " max).");
    }

    // Updates an existing room's bed type, package, or extra guest slots
    private void updateRoom() {
        List<Room> rooms = roomManager.viewAllRooms();
        if (rooms.isEmpty()) { System.out.println("No rooms exist."); return; }
        printRoomTableForAdmin(rooms);
        String id = readLine("\nRoom ID to update (or Enter to cancel)");
        if (id.isEmpty()) { System.out.println("Cancelled."); return; }
        Room existing = roomManager.findById(id);
        if (existing == null) { System.out.println("No room with ID " + id + "."); return; }

        // Show current configuration before modification
        System.out.println("Current: " + existing.getRoomPackage().getPackageTypeName()
                + ", " + existing.getBedType() + ", " + existing.getAvailabilityStatus()
                + ", " + existing.getRoomPackage().getBaseCapacity()
                + " included + " + (existing.getMaxOccupancy() - existing.getRoomPackage().getBaseCapacity())
                + " extra");

        System.out.println("What do you want to change?");
        System.out.println("  1. Bed type");
        System.out.println("  2. Room type (package)");
        System.out.println("  3. Extra guest slots");
        System.out.println("  4. All three");
        switch (readInt("Choice")) {
            case 1 -> existing.setBedType(pickBedType());
            case 2 -> {
                RoomPackage pkg = pickPackage();
                if (pkg == null) return;
                existing.setRoomPackage(pkg);
                // Re-derive max occupancy if the new package's base capacity is larger
                int oldMax = existing.getMaxOccupancy();
                if (oldMax < pkg.getBaseCapacity()) {
                    System.out.println("Note: " + pkg.getPackageTypeName()
                            + " includes " + pkg.getBaseCapacity()
                            + " guests. Current max was " + oldMax
                            + " — adjusting to " + pkg.getBaseCapacity()
                            + " + 0 extra = " + pkg.getBaseCapacity() + ".");
                    existing.setMaxOccupancy(pkg.getBaseCapacity());
                }
            }
            case 3 -> {
                int extra = readExtraSlots(existing.getRoomPackage());
                existing.setMaxOccupancy(existing.getRoomPackage().getBaseCapacity() + extra);
            }
            case 4 -> {
                existing.setBedType(pickBedType());
                RoomPackage pkg = pickPackage();
                if (pkg == null) return;
                existing.setRoomPackage(pkg);
                int extra = readExtraSlots(pkg);
                existing.setMaxOccupancy(pkg.getBaseCapacity() + extra);
            }
            default -> { System.out.println("Invalid choice."); return; }
        }
        roomManager.updateRoom(existing);
        System.out.println("Room " + id + " updated.");
    }

    // Removes a room, warning if it has active reservations
    private void removeRoom() {
        List<Room> rooms = roomManager.viewAllRooms();
        if (rooms.isEmpty()) { System.out.println("No rooms exist."); return; }
        printRoomTableForAdmin(rooms);
        String id = readLine("\nRoom ID to remove (or Enter to cancel)");
        if (id.isEmpty()) { System.out.println("Cancelled."); return; }
        Room existing = roomManager.findById(id);
        if (existing == null) { System.out.println("No room with ID " + id + "."); return; }

        // Check for active bookings on this room
        boolean hasActive = bookingManager.getAllBookings().stream()
                .anyMatch(b -> b.isActive()
                        && b.getRoom().getRoomId().equalsIgnoreCase(id));
        if (hasActive) {
            System.out.println("Cannot remove: room " + id + " has active reservations.");
            if (!confirm("Remove anyway (leaves orphan bookings)?")) {
                System.out.println("Cancelled."); return;
            }
        } else if (!confirm("Remove room " + id + "?")) {
            System.out.println("Cancelled."); return;
        }
        roomManager.removeRoom(id);
        System.out.println("Room " + id + " removed.");
    }

    // Changes a room's availability status, warning about affected bookings
    private void changeRoomStatus() {
        List<Room> rooms = roomManager.viewAllRooms();
        if (rooms.isEmpty()) { System.out.println("No rooms exist."); return; }
        printRoomTableForAdmin(rooms);
        String id = readLine("\nRoom ID to change status (or Enter to cancel)");
        if (id.isEmpty()) { System.out.println("Cancelled."); return; }
        Room existing = roomManager.findById(id);
        if (existing == null) { System.out.println("No room with ID " + id + "."); return; }

        String old = existing.getAvailabilityStatus();
        System.out.println("Current status: " + old);
        System.out.println("New status:");
        System.out.println("  1. AVAILABLE");
        System.out.println("  2. UNDER MAINTENANCE");
        String newStatus;
        // Keep asking until a valid status is chosen
        while (true) {
            switch (readInt("Choice")) {
                case 1 -> { newStatus = "AVAILABLE"; break; }
                case 2 -> { newStatus = "UNDER MAINTENANCE"; break; }
                default -> { System.out.println("Invalid - choose 1-2."); continue; }
            }
            break;
        }

        // Warn if switching to maintenance affects existing future bookings
        if ("UNDER MAINTENANCE".equalsIgnoreCase(newStatus)) {
            List<Booking> affected = bookingManager.getAllBookings().stream()
                    .filter(b -> b.isActive()
                            && b.getRoom().getRoomId().equalsIgnoreCase(id)
                            && !b.getCheckOutDate().isBefore(LocalDate.now()))
                    .collect(Collectors.toList());
            if (!affected.isEmpty()) {
                System.out.println("\nWARNING: " + affected.size()
                        + " future/current booking(s) exist for room " + id + ":");
                printBookingsTable(affected);
                if (!confirm("Proceed anyway? Guests must be contacted separately.")) {
                    System.out.println("Status change aborted."); return;
                }
            }
        }

        roomManager.manageRoomStatus(id, newStatus);
        System.out.println("Room " + id + " status: " + old + " -> " + newStatus);
    }

    // Searches all reservations by guest name
    private void searchReservationsByName() {
        List<Booking> list = bookingManager.searchReservationByName(readLine("Guest name"));
        if (list.isEmpty()) { System.out.println("No reservations."); return; }
        list.forEach(this::printBooking);
    }

    // Shows all reservations on a given check-in date
    private void viewReservationsByDate() {
        List<Booking> list = bookingManager.viewReservationsByDate(
                readDate("Check-in date (YYYY-MM-DD)"));
        if (list.isEmpty()) { System.out.println("No reservations."); return; }
        list.forEach(this::printBooking);
    }

    // Shows every booking in the system
    private void showAllBookings() {
        List<Booking> list = bookingManager.getAllBookings();
        if (list.isEmpty()) { System.out.println("No bookings yet."); return; }
        System.out.println("\n--- All Bookings (" + list.size() + ") ---");
        list.forEach(this::printBooking);
    }

   // ===========================
    // HELPERS
   // ===========================

    // Prompts the admin to choose a bed type
    private String pickBedType() {
        while (true) {
            System.out.println("Bed type:");
            System.out.println("  1. Single");
            System.out.println("  2. Double");
            System.out.println("  3. Queen");
            System.out.println("  4. King");
            switch (readInt("Choice")) {
                case 1 -> { return "Single"; }
                case 2 -> { return "Double"; }
                case 3 -> { return "Queen"; }
                case 4 -> { return "King"; }
                default -> System.out.println("Invalid - choose 1-4.");
            }
        }
    }

    // Prompts the admin to choose a room package type
    private RoomPackage pickPackage() {
        System.out.println("Room type: 1. Standard 2. Deluxe 3. Suite 4. Family Suite");
        return switch (readInt("Choice")) {
            case 1 -> new Standard();
            case 2 -> new Deluxe();
            case 3 -> new Suite();
            case 4 -> new FamilySuite();
            default -> { System.out.println("Invalid type."); yield null; }
        };
    }

    // Prints a table of rooms for admin view, including capacity details
    private void printRoomTableForAdmin(List<Room> rooms) {
        System.out.println();
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-6s %-13s %-9s %-18s %s%n",
                "ID", "Room Type", "Bed", "Status", "Capacity");
        System.out.println("--------------------------------------------------------------------------------");
        for (Room r : rooms) {
            RoomPackage p = r.getRoomPackage();
            int included = p.getBaseCapacity();
            int extraSlots = r.getMaxOccupancy() - included;

            // Show how many guests are included vs. allowed as extras
            String line1 = (extraSlots > 0)
                    ? included + " included + " + extraSlots + " extra"
                    : included + " included (no extra guests)";

            System.out.printf("%-6s %-13s %-9s %-18s %s%n",
                    r.getRoomId(),
                    p.getPackageTypeName(),
                    r.getBedType(),
                    r.getAvailabilityStatus(),
                    line1);

            // Show extra guest charge on a second line when applicable
            if (extraSlots > 0) {
                System.out.printf("%-6s %-13s %-9s %-18s LKR %,.0f / extra guest%n",
                        "", "", "", "", p.getExtraGuestCharge());
            }

            System.out.println();
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    // Prints a table of rooms for guest view, including rate and breakfast info
    private void printRoomTableForGuest(List<Room> rooms, LocalDate in, LocalDate out) {
        System.out.println();
        if (in != null && out != null) {
            System.out.println("Rooms available from " + in + " to " + out + ":");
        }
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-6s %-13s %-9s %12s   %-10s %s%n",
                "ID", "Room Type", "Bed", "Rate/Night", "Breakfast", "Guest Details");
        System.out.println("--------------------------------------------------------------------------------");
        for (Room r : rooms) {
            RoomPackage p = r.getRoomPackage();
            int included = p.getBaseCapacity();
            int extraSlots = r.getMaxOccupancy() - included;

            // Show capacity breakdown for the guest
            String line1 = (extraSlots > 0)
                    ? included + " included + " + extraSlots + " extra"
                    : included + " included (no extra guests)";

            System.out.printf("%-6s %-13s %-9s %12s   %-10s %s%n",
                    r.getRoomId(),
                    p.getPackageTypeName(),
                    r.getBedType(),
                    String.format("%,.2f", p.getDailyRate()),
                    p.getBreakfastIncluded() ? "Yes" : "No",
                    line1);

            // Show extra guest charge on a second line when applicable
            if (extraSlots > 0) {
                System.out.printf("%-6s %-13s %-9s %12s   %-10s LKR %,.0f / extra guest%n",
                        "", "", "", "", "", p.getExtraGuestCharge());
            }

            System.out.println();
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    // Displays rooms plus current and upcoming bookings for the admin
    private void viewRoomsAndBookings() {
        System.out.println("\n=== ROOMS ===");
        printRoomTableForAdmin(roomManager.viewAllRooms());

        // Currently staying guests (booking date range covers today)
        System.out.println("\n=== CURRENTLY STAYING (bookings covering today " + LocalDate.now() + ") ===");
        List<Booking> now = bookingManager.currentBookings();
        if (now.isEmpty()) {
            System.out.println("(none)");
        } else {
            printBookingsTable(now);
        }

        // Future bookings (check-in after today)
        System.out.println("\n=== UPCOMING BOOKINGS (future check-in) ===");
        List<Booking> upcoming = bookingManager.upcomingBookings();
        if (upcoming.isEmpty()) {
            System.out.println("(none)");
        } else {
            printBookingsTable(upcoming);
        }
    }

    // Prints a compact table of bookings for admin views
    private void printBookingsTable(List<Booking> list) {
        System.out.printf("%-18s %-6s %-14s %-12s %-12s %-6s %-10s%n",
                "Booking ID", "Room", "Guest", "Check-in", "Check-out", "Guests", "Status");
        System.out.println("-".repeat(82));
        for (Booking b : list) {
            System.out.printf("%-18s %-6s %-14s %-12s %-12s %-6d %-10s%n",
                    b.getBookingId(),
                    b.getRoom().getRoomId(),
                    truncate(b.getGuest().getName(), 14),
                    b.getCheckInDate(),
                    b.getCheckOutDate(),
                    b.getNumberOfGuests(),
                    b.getBookingStatus());
        }
    }

    // Shortens a string to fit within a column width, appending an ellipsis if needed
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // Prints full details of a single booking, including computed costs
    private void printBooking(Booking b) {
        System.out.println("-------------------------------------");
        System.out.println("Booking ID : " + b.getBookingId());
        System.out.println("Guest      : " + b.getGuest().getName());
        System.out.println("Room       : " + b.getRoom().getRoomId()
                + " (" + b.getRoom().getRoomPackage().getPackageTypeName() + ")");
        System.out.println("Check-in   : " + b.getCheckInDate());
        System.out.println("Check-out  : " + b.getCheckOutDate());
        System.out.println("Guests     : " + b.getNumberOfGuests());

        // Append explanatory text to statuses that involve a refund
        String statusText = b.getBookingStatus();
        if ("COMPLETED".equalsIgnoreCase(statusText)) {
            statusText += " (stay finished, advance refunded)";
        } else if ("CANCELLED".equalsIgnoreCase(statusText)) {
            statusText += " (cancelled, advance refunded)";
        }
        System.out.println("Status     : " + statusText);

        // Compute stay cost via Invoice and show it alongside the advance
        Invoice inv = new Invoice(b);
        inv.calculateBill();
        double stayCost = inv.getFinalPayableAmount();
        double balance = stayCost - b.getAdvancePaid();

        System.out.printf("Stay Cost  : LKR %10.2f%n", stayCost);
        System.out.printf("Advance    : LKR %10.2f (deposit)%n", b.getAdvancePaid());
        if (balance > 0) {
            System.out.printf("Balance    : LKR %10.2f (due at checkout)%n", balance);
        } else if (balance < 0) {
            System.out.printf("Refund     : LKR %10.2f (owed to guest)%n", -balance);
        } else {
            System.out.println("Balance    : LKR       0.00");
        }
    }

    // Reads a trimmed line of input from the console
    private String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return sc.nextLine().trim();
    }

    // Reads an integer, repeating until a valid number is entered
    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String s = sc.nextLine().trim();
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { System.out.println("Enter a number."); }
        }
    }

    // Reads a date in YYYY-MM-DD format, repeating until valid
    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String s = sc.nextLine().trim();
            try { return LocalDate.parse(s); }
            catch (Exception e) { System.out.println("Invalid date, use YYYY-MM-DD."); }
        }
    }

    // Asks a yes/no question and returns true for Y or YES
    private boolean confirm(String prompt) {
        String ans = readLine(prompt + " (Y/N)").toUpperCase();
        return ans.equals("Y") || ans.equals("YES");
    }

    // Reads a non-empty string, repeating until the user enters something
    private String requireNonBlank(String prompt) {
        while (true) {
            String v = readLine(prompt);
            if (!v.isEmpty()) return v;
            System.out.println("This field is required.");
        }
    }

    // Reads an email address, validating its basic format
    private String requireEmail(String prompt) {
        while (true) {
            String v = readLine(prompt);
            if (v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return v;
            System.out.println("Invalid email format (e.g. user@mail.com).");
        }
    }

    // Reads a contact number, requiring 9 to 15 digits
    private String requireContact(String prompt) {
        while (true) {
            String v = readLine(prompt);
            if (v.matches("\\d{9,15}")) return v;
            System.out.println("Enter 9-15 digits.");
        }
    }

    // Reads the number of extra guest slots allowed, with sanity checks
    private int readExtraSlots(RoomPackage pkg) {
        int base = pkg.getBaseCapacity();
        while (true) {
            int extra = readInt("Extra guests allowed (base " + base + ")");
            if (extra < 0) {
                System.out.println("Cannot be negative.");
                continue;
            }
            if (extra > 5) {
                System.out.println("Sanity cap: at most 5 extra guests per room.");
                continue;
            }
            return extra;
        }
    }
}