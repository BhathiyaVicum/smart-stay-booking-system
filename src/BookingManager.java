import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BookingManager {
    // Stores bookings keyed by uppercase booking ID to allow case-insensitive lookups
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    // Reference to the room manager, used for room lookups and availability checks
    private final RoomManager roomManager;

    // Constructor receives the room manager dependency
    public BookingManager(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    // Checks whether a specific room has no active overlapping booking for the given dates
    public boolean isRoomAvailableForDates(Room room, LocalDate in, LocalDate out) {
        for (Booking b : bookings.values()) {
            // A conflict exists if the booking is on the same room, active, and dates overlap
            if (b.getRoom().getRoomId().equalsIgnoreCase(room.getRoomId())
                    && b.isActive()
                    && b.overlaps(in, out)) {
                return false;
            }
        }
        return true;
    }

    // Returns rooms matching the package type and available for the given date range
    public List<Room> searchAvailableRoomsForDates(String type,
                                                   LocalDate in, LocalDate out) {
        return roomManager.viewAllRooms().stream()
                // Exclude rooms under maintenance
                .filter(r -> !"UNDER MAINTENANCE".equalsIgnoreCase(r.getAvailabilityStatus()))
                // Filter by package type if provided
                .filter(r -> type == null || type.isEmpty()
                        || r.getRoomPackage().getPackageTypeName().equalsIgnoreCase(type))
                // Filter by date availability if dates are provided
                .filter(r -> in == null || out == null
                        || isRoomAvailableForDates(r, in, out))
                .collect(Collectors.toList());
    }

    // Bookings that are active and whose date range covers today (guest is currently staying)
    public List<Booking> currentBookings() {
        LocalDate today = LocalDate.now();
        return bookings.values().stream()
                .filter(Booking::isActive)
                .filter(b -> !today.isBefore(b.getCheckInDate())
                        && today.isBefore(b.getCheckOutDate()))
                .collect(Collectors.toList());
    }

    // Bookings that are active and start in the future
    public List<Booking> upcomingBookings() {
        LocalDate today = LocalDate.now();
        return bookings.values().stream()
                .filter(Booking::isActive)
                .filter(b -> today.isBefore(b.getCheckInDate()))
                .collect(Collectors.toList());
    }

    // Like isRoomAvailableForDates, but ignores the given booking (used during modification)
    public boolean isRoomAvailableForDatesExcluding(Room room,
                                                    LocalDate in, LocalDate out,
                                                    Booking ignore) {
        for (Booking b : bookings.values()) {
            // Skip the booking being modified
            if (b == ignore) continue;
            if (b.getRoom().getRoomId().equalsIgnoreCase(room.getRoomId())
                    && b.isActive()
                    && b.overlaps(in, out)) {
                return false;
            }
        }
        return true;
    }

    // Validates and registers a new booking
    public boolean makeReservation(Booking booking) {
        // Reject if the booking does not meet advance booking rules
        if (!booking.isValidAdvanceBooking()) {
            System.out.println("Reservation rejected: " + rejectionReason(booking));
            return false;
        }
        // Reject if the room already has a conflicting reservation
        if (!isRoomAvailableForDates(booking.getRoom(),
                booking.getCheckInDate(),
                booking.getCheckOutDate())) {
            System.out.println("Reservation rejected: room "
                    + booking.getRoom().getRoomId()
                    + " already has a conflicting reservation for those dates.");
            return false;
        }
        // Store the booking keyed by uppercase booking ID
        bookings.put(booking.getBookingId().toUpperCase(), booking);
        return true;
    }

    // Determines the reason a booking failed validation, for display purposes
    private String rejectionReason(Booking b) {
        if (!b.getCheckInDate().isBefore(b.getCheckOutDate()))
            return "check-out must be after check-in";
        if ("UNDER MAINTENANCE".equalsIgnoreCase(b.getRoom().getAvailabilityStatus()))
            return "room " + b.getRoom().getRoomId() + " is under maintenance";
        if (LocalDate.now().plusDays(Booking.MIN_ADVANCE_DAYS).isAfter(b.getCheckInDate()))
            return "must book at least " + Booking.MIN_ADVANCE_DAYS + " days in advance";
        return "unknown";
    }

    // Validates and applies changes to an existing booking
    public boolean modifyReservation(Booking booking,
                                     LocalDate newIn, LocalDate newOut, int newGuests) {
        // Modifications must be within the allowed cutoff window
        if (!booking.isEligibleForModification()) {
            System.out.println("Cannot modify within 24 hours of check-in.");
            return false;
        }
        // Check-out must be after check-in
        if (!newOut.isAfter(newIn)) {
            System.out.println("Check-out must be after check-in.");
            return false;
        }
        // New check-in must satisfy the minimum advance days rule
        if (newIn.isBefore(LocalDate.now().plusDays(Booking.MIN_ADVANCE_DAYS))) {
            System.out.println("New check-in must be at least "
                    + Booking.MIN_ADVANCE_DAYS + " days from today.");
            return false;
        }
        // Guest count must be positive
        if (newGuests <= 0) {
            System.out.println("Number of guests must be positive.");
            return false;
        }
        // Guest count must not exceed the room's capacity
        if (newGuests > booking.getRoom().getMaxOccupancy()) {
            System.out.println("Room supports at most "
                    + booking.getRoom().getMaxOccupancy() + " guests.");
            return false;
        }
        // Ensure no other active booking on the same room overlaps the new dates
        for (Booking other : bookings.values()) {
            if (other == booking) continue;
            if (other.isActive()
                    && other.getRoom().getRoomId().equalsIgnoreCase(
                    booking.getRoom().getRoomId())
                    && other.overlaps(newIn, newOut)) {
                System.out.println("Conflict: another booking on that room overlaps.");
                return false;
            }
        }
        // Apply the validated changes
        booking.applyChanges(newIn, newOut, newGuests);
        System.out.println("Reservation " + booking.getBookingId() + " updated.");
        return true;
    }

    // Returns all bookings made by a guest with the given name
    public List<Booking> searchReservationByName(String name) {
        return bookings.values().stream()
                .filter(b -> b.getGuest().getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }

    // Finds a booking by its ID (case-insensitive), returns null if not found
    public Booking searchReservationById(String id) {
        return id == null ? null : bookings.get(id.toUpperCase());
    }

    // Returns all bookings whose check-in date matches the given date
    public List<Booking> viewReservationsByDate(LocalDate date) {
        return bookings.values().stream()
                .filter(b -> date.equals(b.getCheckInDate()))
                .collect(Collectors.toList());
    }

    // Cancels a booking if it is eligible, and reports the refund
    public void cancelReservation(String bookingId) {
        Booking b = searchReservationById(bookingId);
        if (b == null) {
            System.out.println("Booking not found: " + bookingId);
            return;
        }
        if (!b.isEligibleForCancellation()) {
            System.out.println("Cannot cancel within 24 hours of check-in.");
            return;
        }
        b.cancelBooking();
        System.out.println("Booking cancelled: " + bookingId
                + " — advance of LKR " + b.getAdvancePaid() + " refunded.");
    }

    // Returns a list of all bookings
    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings.values());
    }

    // Returns all bookings belonging to a specific guest (matched by email)
    public List<Booking> getBookingsForGuest(Guest guest) {
        return bookings.values().stream()
                .filter(b -> b.getGuest().getEmail().equalsIgnoreCase(guest.getEmail()))
                .collect(Collectors.toList());
    }
}