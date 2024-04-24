package comfortable_andy.minimap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.map.*;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

import static org.bukkit.util.NumberConversions.round;

@SuppressWarnings({"UnstableApiUsage"})
public class MinimapRenderer extends MapRenderer {

    public MinimapRenderer() {
        super(true);
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        final MapCursorCollection cursorCollection = new MapCursorCollection();

        cursorCollection.addCursor(new MapCursor((byte) 0, (byte) 0, (byte) 0, MapCursor.Type.WHITE_CIRCLE, true));

        // so that NORMAL would be a scale factor of 1
        // since NORMAL has a value of 2, and 1 << 2 = 4
        // (i don't know what i am doing here)
        final double scaleFactor = (map.getScale().getValue() + 1) / 4f;

        final Location playerLocation = player.getLocation();
        final World world = player.getWorld();
        // 180deg here is north, while 0deg here is south
        // why
        final double playerYawRadians = Math.toRadians((playerLocation.getYaw() + 180) % 360);

        renderBlocks(canvas, scaleFactor, playerYawRadians, playerLocation, world);
//        insertEntities(player, playerYawRadians, scaleFactor, cursorCollection);

        canvas.setCursors(cursorCollection);
    }

    private void renderBlocks(@NotNull MapCanvas canvas, double scaleFactor, double playerYawRadians, Location playerLocation, World world) {
        // goal -> render blocks around the player
        final Vector origin = playerLocation.toVector();
        final Vector forward = playerLocation.getDirection()
                .setY(0).normalize().multiply(scaleFactor); // normalize so the up down doesn't matter
        final Vector right = forward.clone().rotateAroundY(Math.toRadians(-90));

        final Vector current = origin.clone(), curForward = forward.clone(), curRight = right.clone();

        for (int x = -64; x < 64; x++) {
            // x and y here act as both the offset to get the blocks
            // and the pixel position to draw to

            int lastHeight = 0;

            for (int y = -64; y < 64; y++) {
                curForward.copy(forward);
                curRight.copy(right);
                current.copy(origin)
                        .add(curForward.multiply(-y))
                        .add(curRight.multiply(x));

                final int blockX = current.getBlockX();
                final int blockZ = current.getBlockZ();

                final Block block = world.getHighestBlockAt(blockX, blockZ);

                final int height = block.getY();
                final int deltaHeight = height - lastHeight;

                final float brightness = switch (
                        Math.round(deltaHeight / (deltaHeight == 0 ? 1f : deltaHeight) * Math.signum(deltaHeight))
                        ) {
                    case 1 -> 1f;
                    case 0 -> 0.75f;
                    default -> 0.5f;
                };

                canvas.setPixelColor(x + 64, y + 64,
                        setBrightness(block.getBlockData().getMapColor(), brightness));

                lastHeight = height;
            }
        }
    }

    private void insertEntities(@NotNull Player player, double playerYawRadians, double scaleFactor, MapCursorCollection cursorCollection) {
        // goal -> render entities around the player as cursors
        //         (up is north, down is south)
        final Location playerLocation = player.getLocation();
        final double reach = 64, playerYaw = playerLocation.getYaw() + 180;

        final XYPair cosSin = new XYPair(Math.cos(-playerYawRadians), Math.sin(-playerYawRadians));

        for (Entity entity : player.getNearbyEntities(reach, reach, reach)) {
            if (!(entity instanceof LivingEntity) || entity instanceof Bat) continue;
            if (entity == player) continue;

            final Location entityLocation = entity.getLocation();

            // divide because each pixel would count for more blocks/distance
            // meaning entities should appear to be closer the larger the scale is
            int relativeX = round((entityLocation.getBlockX() - playerLocation.getBlockX()) / scaleFactor);
            int relativeZ = round((entityLocation.getBlockZ() - playerLocation.getBlockZ()) / scaleFactor);

            final XYPair rotated = rotatePoint(new XYPair(relativeX, relativeZ), cosSin);

            cursorCollection.addCursor(
                    new MapCursor(
                            // map cursor spans from -128 to 127, 2 times more precise than map pixel
                            // so these two are multiplied by 2 (+ clamped)
                            (byte) clamp(-128, rotated.x * 2, 127),
                            (byte) clamp(-128, rotated.y * 2, 127),
                            getDirection(entityLocation.getYaw() - playerYaw),
                            entity instanceof Monster ? MapCursor.Type.RED_POINTER : MapCursor.Type.GREEN_POINTER,
                            true
                    )
            );
        }
    }

    private java.awt.Color setBrightness(@NotNull final org.bukkit.Color original, float alpha) {
        alpha = (float) clamp(0, alpha, 1);
        return new java.awt.Color(
                (int) (original.getRed() * alpha),
                (int) (original.getGreen() * alpha),
                (int) (original.getBlue() * alpha)
        );
    }

    private byte getDirection(final double yaw) {
        return (byte) ((int) ((yaw / 360) * 16) & 15);
    }

    private XYPair rotatePoint(XYPair coord, XYPair cosSin) {
        return new XYPair((coord.x * cosSin.x - coord.y * cosSin.y), (coord.y * cosSin.x + coord.x * cosSin.y));
    }

    private double clamp(double min, double val, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public record XYPair(double x, double y) {
    }

    public record Box(int x, int y) {

        public Box(int x, int y) {
            this.x = Math.abs(x);
            this.y = Math.abs(y);
        }

        public boolean isIn(int cx, int cy) {
            return x > cx && -x < cx && y > cy && -y < cy;
        }
    }

}

