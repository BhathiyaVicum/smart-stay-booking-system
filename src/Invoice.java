import java.text.NumberFormat;

public class Invoice {

    private double baseCharge;
    private double extraGuestTotal;
    private double discountAmount;
    private double taxAmount;
    private double advanceDeducted;
    private double finalPayableAmount;

    // Booking this invoice is generated for
    private final Booking booking;

    // Constructor stores the booking reference
    public Invoice(Booking booking) {
        this.booking = booking;
    }

    // Computes all billing components for the booking
    public void calculateBill() {
        RoomPackage pkg = booking.getRoom().getRoomPackage();
        long nights = booking.calculateNights();

        // 1) Base room charge: daily rate multiplied by number of nights
        baseCharge = pkg.getDailyRate() * nights;

        // 2) Extra guest charge: only for guests above the package base capacity
        int extraGuests = Math.max(0, booking.getNumberOfGuests() - pkg.getBaseCapacity());
        extraGuestTotal = extraGuests * pkg.getExtraGuestCharge() * nights;

        // 3) 12% discount applied to the base charge for stays of 5 nights or more
        discountAmount = (nights >= 5) ? baseCharge * 0.12 : 0.0;

        // 4) Tax is applied on (base + extra guest charges - discount)
        double taxable = baseCharge + extraGuestTotal - discountAmount;
        taxAmount = taxable * pkg.getTaxRate();

        // 5) Advance paid is treated as a refundable deposit
        advanceDeducted = booking.getAdvancePaid();
        finalPayableAmount = taxable + taxAmount;
    }

    // Prints a detailed invoice, or a refund notice if the booking was cancelled
    public void generateDetailedInvoice() {
        // Refund path for cancelled bookings
        if ("CANCELLED".equalsIgnoreCase(booking.getBookingStatus())) {
            System.out.println("\n========== SMARTSTAY - REFUND NOTICE ==========");
            System.out.println("Booking ID  : " + booking.getBookingId());
            System.out.println("Guest       : " + booking.getGuest().getName());
            System.out.println("Room        : " + booking.getRoom().getRoomId());
            System.out.println("This booking was CANCELLED.");
            System.out.printf ("Refund due  : LKR %.2f%n", booking.getAdvancePaid());
            System.out.println("================================================");
            return;
        }

        Room  room  = booking.getRoom();
        Guest guest = booking.getGuest();

        // Room details section
        System.out.println("\n========== SMARTSTAY INVOICE ==========");
        System.out.println("------------- Room Details -------------");
        System.out.println("Room ID    : " + room.getRoomId());
        System.out.println("Room Type  : " + room.getRoomPackage().getPackageTypeName());
        System.out.println("Bed Type   : " + room.getBedType());
        System.out.printf ("Daily Rate : LKR %.2f%n", room.getRoomPackage().getDailyRate());
        System.out.println("Breakfast  : "
                + (room.getRoomPackage().getBreakfastIncluded() ? "Included" : "Not included"));

        // Guest details section
        System.out.println("------------- Guest Details -------------");
        System.out.println("Name       : " + guest.getName());
        System.out.println("NIC        : " + guest.getNic());
        System.out.println("Email      : " + guest.getEmail());
        System.out.println("Contact    : " + guest.getContactNumber());

        // Stay details section
        System.out.println("------------- Stay Details -------------");
        System.out.println("Check-in   : " + booking.getCheckInDate());
        System.out.println("Check-out  : " + booking.getCheckOutDate());
        System.out.println("Nights     : " + booking.calculateNights());
        System.out.println("Guests     : " + booking.getNumberOfGuests());

        // Charges breakdown
        System.out.println("------------- Charges -------------");
        System.out.printf ("Base Charge       : LKR %10.2f%n", baseCharge);
        System.out.printf ("Extra Guest Total : LKR %10.2f%n", extraGuestTotal);
        // Show discount only if it applies
        if (discountAmount > 0.0) {
            System.out.printf("Discount (12%%)    : LKR %10.2f%n", -discountAmount);
        } else {
            System.out.println("Discount          : Not applicable (< 5 nights)");
        }
        System.out.printf ("Tax (%.0f%%)         : LKR %10.2f%n",
                room.getRoomPackage().getTaxRate() * 100, taxAmount);
        System.out.println("---------------------------------------");
        System.out.printf("TOTAL STAY COST    : LKR %10.2f%n", finalPayableAmount);
        System.out.printf("ADVANCE PAID       : LKR %10.2f%n", advanceDeducted);

        // Remaining balance is the total cost minus the advance paid
        double balance = finalPayableAmount - advanceDeducted;

        System.out.println("---------------------------------------");

        // Report remaining payment, refund, or a zero balance
        if (balance > 0) {
            System.out.printf("REMAINING PAYMENT  : LKR %10.2f%n", balance);
        } else if (balance < 0) {
            System.out.printf("REFUND DUE         : LKR %10.2f%n", -balance);
        } else {
            System.out.println("REMAINING PAYMENT  : LKR       0.00");
        }
        System.out.println("=======================================");
    }

    // Getter for the associated booking
    public Booking getBooking() {
        return booking;
    }

    // Getter for the final payable amount
    public double getFinalPayableAmount() {
        return finalPayableAmount;
    }
}