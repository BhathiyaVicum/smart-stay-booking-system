import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Booking {
    public static final double ADVANCE_AMOUNT = 10_000.00; // Fixed advance amount required for a booking
    public static final int MIN_ADVANCE_DAYS = 2; // Minimum number of days in advance a booking must be made
    public static final int CANCEL_CUTOFF_HOURS = 24; // Hours before check-in after which cancellation is not allowed

    private String bookingId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int numberOfGuests;
    private double advancePaid;
    private String bookingStatus;
    private Guest guest;
    private Room room;

    // Constructor initializes booking details and sets the advance amount automatically
    public Booking(String bookingId, LocalDate checkInDate, LocalDate checkOutDate, int numberOfGuests,
                   String bookingStatus, Guest guest, Room room) {
        this.bookingId = bookingId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfGuests = numberOfGuests;
        this.bookingStatus = bookingStatus;
        this.guest = guest;
        this.room = room;
        this.advancePaid = ADVANCE_AMOUNT;
    }

    // Calculates the number of nights between check-in and check-out
    public long calculateNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    // Validates that the booking meets advance booking rules
    public boolean isValidAdvanceBooking() {
        // Check-in and check-out dates must be set
        if (checkInDate == null || checkOutDate == null) return false;
        // Check-out must be after check-in
        if (!checkInDate.isBefore(checkOutDate)) return false;
        // Room must not be under maintenance
        if ("UNDER MAINTENANCE".equalsIgnoreCase(room.getAvailabilityStatus())) return false;
        // Booking must be made at least MIN_ADVANCE_DAYS in advance
        return !LocalDate.now().plusDays(MIN_ADVANCE_DAYS).isAfter(checkInDate);
    }

    // Checks whether this booking's date range overlaps with another date range
    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return this.checkInDate.isBefore(otherOut)
                && otherIn.isBefore(this.checkOutDate);
    }

    // Returns true if the booking is not cancelled
    public boolean isActive() {
        return !"CANCELLED".equalsIgnoreCase(bookingStatus);
    }

    // Checks whether the booking can still be cancelled based on the cutoff time
    public boolean isEligibleForCancellation() {
        if (!isActive()) return false;
        // Cancellation must occur before the cutoff time prior to check-in
        LocalDateTime cutoff = checkInDate.atStartOfDay().minusHours(CANCEL_CUTOFF_HOURS);
        return LocalDateTime.now().isBefore(cutoff);
    }

    // Modification is allowed under the same conditions as cancellation
    public boolean isEligibleForModification() {
        return isEligibleForCancellation();
    }

    // Cancels the booking if it is eligible for cancellation
    public void cancelBooking() {
        if (isEligibleForCancellation()) {
            this.bookingStatus = "CANCELLED";
        }
    }

    // Applies new dates and guest count after validating them
    public void applyChanges(LocalDate newIn, LocalDate newOut, int newGuests) {
        // Check-out must be after check-in
        if (!newOut.isAfter(newIn))
            throw new IllegalArgumentException("check-out must be after check-in");
        // Guest count must be positive
        if (newGuests <= 0)
            throw new IllegalArgumentException("guests must be positive");
        this.checkInDate = newIn;
        this.checkOutDate = newOut;
        this.numberOfGuests = newGuests;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public double getAdvancePaid() {
        return advancePaid;
    }

    public void setAdvancePaid(double advancePaid) {
        this.advancePaid = advancePaid;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}