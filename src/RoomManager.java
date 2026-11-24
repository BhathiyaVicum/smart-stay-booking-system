import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoomManager {
    // Stores rooms keyed by their uppercase room ID to allow case-insensitive lookups
    private final Map<String, Room> rooms = new LinkedHashMap<>();

    // Adds a new room to the collection, keyed by uppercase room ID
    public void addRoom(Room room) {
        rooms.put(room.getRoomId().toUpperCase(), room);
    }

    // Updates an existing room (or inserts it if not present) using the same key logic
    public void updateRoom(Room room) {
        rooms.put(room.getRoomId().toUpperCase(), room);
    }

    // Removes a room by its ID (case-insensitive)
    public void removeRoom(String roomId) {
        rooms.remove(roomId.toUpperCase());
    }

    // Updates the availability status of a specific room if it exists
    public void manageRoomStatus(String roomId, String status) {
        Room r = rooms.get(roomId.toUpperCase());
        if (r != null) r.updateStatus(status);
    }

    // Generates the next available room ID in the format R###, starting from R101
    public String generateNextRoomId() {
        int max = 100;   // first is R101
        for (Room r : rooms.values()) {
            String id = r.getRoomId();
            if (id != null && id.startsWith("R")) {
                try {
                    // Extract the numeric part after the 'R' prefix
                    int n = Integer.parseInt(id.substring(1));
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) { }
            }
        }
        return "R" + (max + 1);
    }

    // Returns a list of all rooms in insertion order
    public List<Room> viewAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    // Finds a room by its ID (case-insensitive), returns null if not found
    public Room findById(String roomId) {
        if (roomId == null) return null;
        return rooms.get(roomId.toUpperCase());
    }
}