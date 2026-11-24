public abstract class RoomPackage {
    // Name of the package type (e.g. Standard, Deluxe, Suite)
    private String packageTypeName;

    // Constructor initializes the package type name
    public RoomPackage(String packageTypeName) {
        this.packageTypeName = packageTypeName;
    }

    // Each concrete package defines its own daily rate, tax, guest charge, breakfast included, guest capacity
    public abstract double getDailyRate();
    public abstract double getTaxRate();
    public abstract double getExtraGuestCharge();
    public abstract boolean getBreakfastIncluded();
    public abstract int getBaseCapacity();

    // Getter for package type name
    public String getPackageTypeName() {
        return packageTypeName;
    }

    // Setter for package type name
    public void setPackageTypeName(String packageTypeName) {
        this.packageTypeName = packageTypeName;
    }
}