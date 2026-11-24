public class Deluxe extends RoomPackage {
    public Deluxe() {
        super("Deluxe");
    }

    @Override
    public double getDailyRate() { return 9000.00; }

    @Override
    public double getTaxRate() { return 0.12;}

    @Override
    public double getExtraGuestCharge() { return 1500.00; }

    @Override
    public boolean getBreakfastIncluded() {
        return true;
    }

    @Override
    public int getBaseCapacity() {
        return 2;
    }
}
