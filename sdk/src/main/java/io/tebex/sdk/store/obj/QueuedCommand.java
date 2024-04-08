package io.tebex.sdk.store.obj;

public class QueuedCommand {
    private final int id;
    private final String command;
    private final Integer payment;
    private final Integer packageId; // may be null
    private final Integer delay;
    private final Integer requiredSlots;
    private final QueuedPlayer player;
    private final boolean online;

    public QueuedCommand(int id, String command, int payment, int packageId, int delay, int requiredSlots) {
        this.id = id;
        this.command = command;
        this.payment = payment;
        this.packageId = packageId;
        this.delay = delay;
        this.requiredSlots = requiredSlots;
        this.player = null;
        this.online = true;
    }

    public QueuedCommand(int id, String command, int payment, int packageId, int delay, int requiredSlots, QueuedPlayer player) {
        this.id = id;
        this.command = command;
        this.payment = payment;
        this.packageId = packageId;
        this.delay = delay;
        this.requiredSlots = requiredSlots;
        this.player = player;
        this.online = false;
    }

    public int getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }

    public String getParsedCommand() {
        String parsedCommand = command;

        if (player != null) {
            parsedCommand = parsedCommand.replace("{username}", player.getName());
            parsedCommand = parsedCommand.replace("{name}", player.getName());

            if (player.getUuid() != null) { // offline servers will return null uuid here as these uuids are not verified
                parsedCommand = parsedCommand.replace("{id}", player.getUuid());
                parsedCommand = parsedCommand.replace("{uuid}", player.getUuid());
            } else { // {id} must still be replaced with username if uuid is not present
                parsedCommand = parsedCommand.replace("{id}", player.getName());
            }
        }

        return parsedCommand;
    }

    public int getPayment() {
        return payment == null ? 0 : payment;
    }

    /**
     * The package id relating to this command.
     * @return the package id. 0 if not provided
     */
    public int getPackageId() {
        return packageId == null ? 0 : packageId;
    }

    /**
     * The execution delay required by command.
     * @return the delay in seconds
     */
    public int getDelay() {
        return delay == null ? 0 : delay;
    }

    /**
     * The required slots required by this command.
     * @return the required slots
     */
    public int getRequiredSlots() {
        return requiredSlots == null ? 0 : delay;
    }

    public QueuedPlayer getPlayer() {
        return player;
    }

    public boolean isOnline() {
        return online;
    }


    @Override
    public String toString() {
        return "QueuedCommand{" +
                "id=" + id +
                ", command='" + command + '\'' +
                ", payment=" + payment +
                ", packageId=" + packageId +
                ", delay=" + delay +
                ", requiredSlots=" + requiredSlots +
                ", player=" + player +
                ", online=" + online +
                '}';
    }
}
