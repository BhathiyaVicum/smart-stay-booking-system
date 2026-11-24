public class Room {
    private String roomId;
    private String bedType;
    private String availabilityStatus;
    private RoomPackage roomPackage;

    // Maximum number of guests the room can hold
    private int maxOccupancy;

    // Constructor initializes all room details
    public Room(String roomId, String bedType, String availabilityStatus,
                RoomPackage roomPackage, int maxOccupancy) {
        this.roomId = roomId;
        this.bedType = bedType;
        this.availabilityStatus = availabilityStatus;
        this.roomPackage = roomPackage;
        this.maxOccupancy = maxOccupancy;
    }

    // Updates the availability status of the room
    public void updateStatus(String newStatus) { this.availabilityStatus = newStatus; }

    public String getRoomId() { return roomId; }

    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getBedType() { return bedType; }

    public void setBedType(String bedType) { this.bedType = bedType; }

    public String getAvailabilityStatus() { return availabilityStatus;}

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public RoomPackage getRoomPackage() { return roomPackage; }

    public void setRoomPackage(RoomPackage roomPackage) { this.roomPackage = roomPackage; }

    public int getMaxOccupancy() { return maxOccupancy; }

    public void setMaxOccupancy(int maxOccupancy) { this.maxOccupancy = maxOccupancy;}
}