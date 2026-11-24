public class Standard extends RoomPackage {
    public Standard() {
        super("Standard");
    }

    @Override
    public double getDailyRate() { return 6000.00; }

    @Override
    public double getTaxRate() { return 0.10; }

    @Override
    public double getExtraGuestCharge() { return 1000.00; }

    @Override
    public boolean getBreakfastIncluded() { return false; }

    @Override
    public int getBaseCapacity() { return 2; }
}
