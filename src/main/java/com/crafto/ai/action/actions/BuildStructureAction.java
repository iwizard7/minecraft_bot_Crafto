package com.crafto.ai.action.actions;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.CollaborativeBuildManager;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.inventory.InventoryManager;
import com.crafto.ai.memory.StructureRegistry;
import com.crafto.ai.structure.StructureTemplateLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class BuildStructureAction extends BaseAction {
    private static class BlockPlacement {
        BlockPos pos;
        Block block;
        
        BlockPlacement(BlockPos pos, Block block) {
            this.pos = pos;
            this.block = block;
        }
    }
    
    private String structureType;
    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex;
    private List<Block> buildMaterials;
    private int ticksRunning;
    private int idleTicks; // Count ticks when no blocks are available
    private CollaborativeBuildManager.CollaborativeBuild collaborativeBuild; // For multi-Crafto collaboration
    private boolean isCollaborative;
    private static final int MAX_TICKS = 120000;
    private static final int MAX_IDLE_TICKS = 200; // 10 seconds at 20 TPS
    private static final int BLOCKS_PER_TICK = 1;
    private static final double BUILD_SPEED_MULTIPLIER = 1.5;

    public BuildStructureAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
        this.idleTicks = 0;
    }

    @Override
    protected void onStart() {
        structureType = task.getStringParameter("structure").toLowerCase();
        
        // Normalize structure name: "big house" -> "big-house"
        structureType = structureType.replace(" ", "-");
        
        CraftoMod.LOGGER.info("Building structure: '{}'", structureType);
        
        currentBlockIndex = 0;
        ticksRunning = 0;
        collaborativeBuild = CollaborativeBuildManager.findActiveBuild(structureType);
        if (collaborativeBuild != null) {
            isCollaborative = true;
            
            crafto.setFlying(true);
            
            CraftoMod.LOGGER.info("Crafto '{}' JOINING collaborative build of '{}' ({}% complete) - FLYING & INVULNERABLE ENABLED", 
                crafto.getCraftoName(), structureType, collaborativeBuild.getProgressPercentage());
            
            buildMaterials = new ArrayList<>();
            buildMaterials.add(Blocks.OAK_PLANKS); // Default material
            buildMaterials.add(Blocks.COBBLESTONE);
            buildMaterials.add(Blocks.GLASS_PANE);
            
            // Пополняем инвентарь при присоединении к существующей постройке
            InventoryManager.refillBuildingMaterials(crafto, buildMaterials);
            
            return; // Skip structure generation, just join the existing build
        }
        
        isCollaborative = false;
        
        buildMaterials = new ArrayList<>();
        Object blocksParam = task.getParameter("blocks");
        if (blocksParam instanceof List) {
            List<?> blocksList = (List<?>) blocksParam;
            for (Object blockObj : blocksList) {
                Block block = parseBlock(blockObj.toString());
                if (block != Blocks.AIR) {
                    buildMaterials.add(block);
                }
            }
        }
        
        if (buildMaterials.isEmpty()) {
            String materialName = task.getStringParameter("material", "oak_planks");
            Block block = parseBlock(materialName);
            buildMaterials.add(block != Blocks.AIR ? block : Blocks.OAK_PLANKS);
        }
        
        // Получаем размеры из параметров или используем значения по умолчанию
        Object dimensionsParam = task.getParameter("dimensions");
        int width = 0;  // 0 означает использовать размер по умолчанию
        int height = 0;
        int depth = 0;
        
        if (dimensionsParam instanceof List) {
            List<?> dims = (List<?>) dimensionsParam;
            if (dims.size() >= 3) {
                width = ((Number) dims.get(0)).intValue();
                height = ((Number) dims.get(1)).intValue();
                depth = ((Number) dims.get(2)).intValue();
            }
        } else {
            width = task.getIntParameter("width", 0);
            height = task.getIntParameter("height", 0);
            depth = task.getIntParameter("depth", 0);
        }
        
        // Применяем размеры по умолчанию для типа строения
        com.crafto.ai.structure.DefaultBuildingSizes.BuildingSize finalSize = 
            com.crafto.ai.structure.DefaultBuildingSizes.applyDefaults(structureType, width, height, depth);
        
        width = finalSize.width;
        height = finalSize.height;
        depth = finalSize.depth;
        
        net.minecraft.world.entity.player.Player nearestPlayer = findNearestPlayer();
        BlockPos groundPos;
        
        if (nearestPlayer != null) {
            net.minecraft.world.phys.Vec3 eyePos = nearestPlayer.getEyePosition(1.0F);
            net.minecraft.world.phys.Vec3 lookVec = nearestPlayer.getLookAngle();
            
            net.minecraft.world.phys.Vec3 targetPos = eyePos.add(lookVec.scale(12));
            
            BlockPos lookTarget = new BlockPos(
                (int)Math.floor(targetPos.x),
                (int)Math.floor(targetPos.y),
                (int)Math.floor(targetPos.z)
            );
            
            groundPos = findGroundLevel(lookTarget);
            
            if (groundPos == null) {
                groundPos = findGroundLevel(nearestPlayer.blockPosition().offset(
                    (int)Math.round(lookVec.x * 10),
                    0,
                    (int)Math.round(lookVec.z * 10)
                ));
            }
            
            CraftoMod.LOGGER.info("Building in player's field of view at {} (looking from {} towards {})", 
                groundPos, eyePos, targetPos);
        } else {
            BlockPos buildPos = crafto.blockPosition().offset(2, 0, 2);
            groundPos = findGroundLevel(buildPos);
        }
        
        if (groundPos == null) {
            result = ActionResult.failure("Cannot find suitable ground for building in your field of view");
            return;
        }
        
        CraftoMod.LOGGER.info("Found ground at Y={} (Build starting at {})", groundPos.getY(), groundPos);
        
        BlockPos clearPos = groundPos;
        
        buildPlan = tryLoadFromTemplate(structureType, clearPos);
        
        if (buildPlan == null) {
            CraftoMod.LOGGER.info("No NBT template found for '{}', falling back to procedural generation", structureType);
            // Fall back to procedural generation
            buildPlan = generateBuildPlan(structureType, clearPos, width, height, depth);
        } else {
            CraftoMod.LOGGER.info("Loaded '{}' from NBT template with {} blocks", structureType, buildPlan.size());
        }
        
        if (buildPlan == null || buildPlan.isEmpty()) {
            result = ActionResult.failure("Cannot generate build plan for: " + structureType);
            return;
        }
        
        StructureRegistry.register(clearPos, width, height, depth, structureType);
        
        collaborativeBuild = CollaborativeBuildManager.findActiveBuild(structureType);
        
        if (collaborativeBuild != null) {
            isCollaborative = true;
            CraftoMod.LOGGER.info("Crafto '{}' JOINING existing {} collaborative build at {}", 
                crafto.getCraftoName(), structureType, collaborativeBuild.startPos);
        } else {
            List<CollaborativeBuildManager.BlockPlacement> collaborativeBlocks = new ArrayList<>();
            for (BlockPlacement bp : buildPlan) {
                collaborativeBlocks.add(new CollaborativeBuildManager.BlockPlacement(bp.pos, bp.block));
            }
            
            collaborativeBuild = CollaborativeBuildManager.registerBuild(structureType, collaborativeBlocks, clearPos);
            isCollaborative = true;
            CraftoMod.LOGGER.info("Crafto '{}' CREATED new {} collaborative build at {}", 
                crafto.getCraftoName(), structureType, clearPos);
        }
        
        crafto.setFlying(true);
        
        // Пополняем инвентарь необходимыми материалами
        InventoryManager.refillBuildingMaterials(crafto, buildMaterials);
        
        CraftoMod.LOGGER.info("Crafto '{}' starting COLLABORATIVE build of {} at {} with {} blocks using materials: {} [FLYING ENABLED]", 
            crafto.getCraftoName(), structureType, clearPos, buildPlan.size(), buildMaterials);
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (ticksRunning > MAX_TICKS) {
            crafto.setFlying(false); // Disable flying on timeout
            result = ActionResult.failure("Building timeout");
            return;
        }
        
        if (isCollaborative && collaborativeBuild != null) {
            if (collaborativeBuild.isComplete()) {
                CollaborativeBuildManager.completeBuild(collaborativeBuild.structureId);
                crafto.setFlying(false);
                result = ActionResult.success("Built " + structureType + " collaboratively!");
                return;
            }
            
            for (int i = 0; i < BLOCKS_PER_TICK; i++) {
                CollaborativeBuildManager.BlockPlacement placement = 
                    CollaborativeBuildManager.getNextBlock(collaborativeBuild, crafto.getCraftoName());
                
                if (placement == null) {
                    idleTicks++;
                    
                    if (ticksRunning % 20 == 0) {
                        CraftoMod.LOGGER.info("Crafto '{}' has no more blocks to place! Build {}% complete", 
                            crafto.getCraftoName(), collaborativeBuild.getProgressPercentage());
                    }
                    
                    // Check if the entire build is complete
                    if (collaborativeBuild.isComplete()) {
                        CollaborativeBuildManager.completeBuild(collaborativeBuild.structureId);
                        crafto.setFlying(false);
                        result = ActionResult.success("Built " + structureType + " collaboratively!");
                        return;
                    }
                    
                    // If Crafto has been idle too long, finish the action
                    if (idleTicks > MAX_IDLE_TICKS) {
                        crafto.setFlying(false);
                        result = ActionResult.success("Completed work on " + structureType + " (" + 
                            collaborativeBuild.getProgressPercentage() + "% total progress)");
                        return;
                    }
                    
                    // If build is not complete but this Crafto has no more work, 
                    // wait a bit and check again (maybe other sections become available)
                    break;
                } else {
                    // Reset idle counter when work is found
                    idleTicks = 0;
                }
                
                // Проверяем есть ли нужный блок в инвентаре
                if (!InventoryManager.hasBlock(crafto, placement.block)) {
                    // Пополняем инвентарь если нужного блока нет
                    if (InventoryManager.needsRefill(crafto, placement.block)) {
                        InventoryManager.refillInventory(crafto, placement.block);
                        CraftoMod.LOGGER.info("Crafto '{}' refilled inventory with {}", 
                            crafto.getCraftoName(), placement.block);
                    } else {
                        // Если пополнение не помогло, пропускаем этот блок
                        if (ticksRunning % 40 == 0) {
                            CraftoMod.LOGGER.warn("Crafto '{}' cannot place {} - no blocks available", 
                                crafto.getCraftoName(), placement.block);
                        }
                        continue;
                    }
                }
                
                BlockPos pos = placement.pos;
                double distance = Math.sqrt(crafto.blockPosition().distSqr(pos));
                if (distance > 5) {
                    crafto.teleportTo(pos.getX() + 2, pos.getY(), pos.getZ() + 2);
                    CraftoMod.LOGGER.info("Crafto '{}' teleported to block at {}", crafto.getCraftoName(), pos);
                }
                
                crafto.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                
                crafto.swing(InteractionHand.MAIN_HAND, true);
                
                BlockState existingState = crafto.level().getBlockState(pos);
                
                // Потребляем блок из инвентаря
                if (!InventoryManager.consumeBlock(crafto, placement.block)) {
                    CraftoMod.LOGGER.warn("Crafto '{}' failed to consume {} from inventory", 
                        crafto.getCraftoName(), placement.block);
                    continue;
                }
                
                BlockState blockState = placement.block.defaultBlockState();
                crafto.level().setBlock(pos, blockState, 3);
                
                CraftoMod.LOGGER.info("Crafto '{}' PLACED BLOCK at {} - Total: {}/{}", 
                    crafto.getCraftoName(), pos, collaborativeBuild.getBlocksPlaced(), 
                    collaborativeBuild.getTotalBlocks());
                
                // Particles and sound
                if (crafto.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        15, 0.4, 0.4, 0.4, 0.15
                    );
                    
                    var soundType = blockState.getSoundType(crafto.level(), pos, crafto);
                    crafto.level().playSound(null, pos, soundType.getPlaceSound(), 
                        SoundSource.BLOCKS, 1.0f, soundType.getPitch());
                }
            }
            
            // Периодически проверяем и пополняем инвентарь
            if (ticksRunning % 60 == 0) { // Каждые 3 секунды
                InventoryManager.refillBuildingMaterials(crafto, buildMaterials);
            }
            
            if (ticksRunning % 100 == 0 && collaborativeBuild.getBlocksPlaced() > 0) {
                int percentComplete = collaborativeBuild.getProgressPercentage();
                CraftoMod.LOGGER.info("{} build progress: {}/{} ({}%) - {} Craftos working", 
                    structureType, 
                    collaborativeBuild.getBlocksPlaced(), 
                    collaborativeBuild.getTotalBlocks(), 
                    percentComplete,
                    collaborativeBuild.participatingCraftos.size());
                
                // Логируем статистику инвентаря для отладки
                if (ticksRunning % 200 == 0) {
                    CraftoMod.LOGGER.debug("Crafto '{}' {}", 
                        crafto.getCraftoName(), InventoryManager.getInventoryStats(crafto));
                }
            }
        } else {
            crafto.setFlying(false); // Disable flying on error
            result = ActionResult.failure("Build system error: not in collaborative mode");
        }
    }

    @Override
    protected void onCancel() {
        crafto.setFlying(false); // Disable flying when cancelled
        crafto.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Build " + structureType + " (" + currentBlockIndex + "/" + (buildPlan != null ? buildPlan.size() : 0) + ")";
    }

    private List<BlockPlacement> generateBuildPlan(String type, BlockPos start, int width, int height, int depth) {
        // Используем AdvancedStructureGenerators для всех поддерживаемых типов
        try {
            var advancedBlocks = com.crafto.ai.structure.AdvancedStructureGenerators.generate(type, start, width, height, depth, buildMaterials);
            List<BlockPlacement> blocks = new ArrayList<>();
            for (var advancedBlock : advancedBlocks) {
                blocks.add(new BlockPlacement(advancedBlock.pos, advancedBlock.block));
            }
            if (!blocks.isEmpty()) {
                return blocks;
            }
        } catch (Exception e) {
            CraftoMod.LOGGER.warn("Failed to generate {} using AdvancedStructureGenerators: {}", type, e.getMessage());
        }
        
        // Fallback to legacy methods for compatibility
        return switch (type.toLowerCase()) {
            case "house", "home" -> buildAdvancedHouse(start, width, height, depth);
            case "castle", "catle", "fort" -> buildCastle(start, width, height, depth);
            case "tower" -> buildAdvancedTower(start, width, height);
            case "wall" -> buildWall(start, width, height);
            case "platform" -> buildPlatform(start, width, depth);
            case "barn", "shed" -> buildBarn(start, width, height, depth);
            case "modern", "modern_house" -> buildModernHouse(start, width, height, depth);
            case "box", "cube" -> buildBox(start, width, height, depth);
            default -> {
                CraftoMod.LOGGER.warn("Unknown structure type '{}', building advanced house", type);
                yield buildAdvancedHouse(start, Math.max(5, width), Math.max(4, height), Math.max(5, depth));
            }
        };
    }
    
    private Block getMaterial(int index) {
        return buildMaterials.get(index % buildMaterials.size());
    }

    private List<BlockPlacement> buildHouse(BlockPos start, int width, int height, int depth) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block floorMaterial = getMaterial(0);
        Block wallMaterial = getMaterial(1);
        Block roofMaterial = getMaterial(2);
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), floorMaterial));
            }
        }
        
        for (int y = 1; y < height; y++) {
            for (int x = 0; x < width; x++) {
                blocks.add(new BlockPlacement(start.offset(x, y, 0), wallMaterial)); // Front wall
                blocks.add(new BlockPlacement(start.offset(x, y, depth - 1), wallMaterial)); // Back wall
            }
            for (int z = 1; z < depth - 1; z++) {
                blocks.add(new BlockPlacement(start.offset(0, y, z), wallMaterial)); // Left wall
                blocks.add(new BlockPlacement(start.offset(width - 1, y, z), wallMaterial)); // Right wall
            }
        }
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, height, z), roofMaterial));
            }
        }
        
        return blocks;
    }

    private List<BlockPlacement> buildWall(BlockPos start, int width, int height) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block material = getMaterial(0);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                blocks.add(new BlockPlacement(start.offset(x, y, 0), material));
            }
        }
        return blocks;
    }

    private List<BlockPlacement> buildTower(BlockPos start, int width, int height) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block material = getMaterial(0);
        Block accentMaterial = getMaterial(1);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    // Hollow tower with accent corners
                    if (x == 0 || x == width - 1 || z == 0 || z == width - 1) {
                        boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == width - 1);
                        Block blockToUse = isCorner ? accentMaterial : material;
                        blocks.add(new BlockPlacement(start.offset(x, y, z), blockToUse));
                    }
                }
            }
        }
        return blocks;
    }

    private List<BlockPlacement> buildPlatform(BlockPos start, int width, int depth) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block material = getMaterial(0);
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), material));
            }
        }
        return blocks;
    }

    private List<BlockPlacement> buildBox(BlockPos start, int width, int height, int depth) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block material = getMaterial(0);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    blocks.add(new BlockPlacement(start.offset(x, y, z), material));
                }
            }
        }
        return blocks;
    }
    
    private List<BlockPlacement> buildAdvancedHouse(BlockPos start, int width, int height, int depth) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block floorMaterial = getMaterial(0);
        Block wallMaterial = getMaterial(1);
        Block roofMaterial = getMaterial(2);
        Block windowMaterial = Blocks.GLASS_PANE;
        Block doorMaterial = Blocks.OAK_DOOR;
        
        if (roofMaterial == Blocks.GLASS || roofMaterial == Blocks.GLASS_PANE) {
            roofMaterial = Blocks.OAK_PLANKS; // Force wood roof if glass was selected
        }
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), floorMaterial));
            }
        }
        for (int y = 1; y <= height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == width / 2 && y <= 2) {
                    blocks.add(new BlockPlacement(start.offset(x, y, 0), doorMaterial));
                } else if (y >= 2 && y <= height - 1 && (x == 2 || x == width - 3)) {
                    // Windows on front wall (taller windows)
                    blocks.add(new BlockPlacement(start.offset(x, y, 0), windowMaterial));
                } else {
                    blocks.add(new BlockPlacement(start.offset(x, y, 0), wallMaterial));
                }
                
                // BACK WALL - Multiple windows
                if (y >= 2 && y <= height - 1 && (x == 2 || x == width / 2 || x == width - 3)) {
                    blocks.add(new BlockPlacement(start.offset(x, y, depth - 1), windowMaterial));
                } else {
                    blocks.add(new BlockPlacement(start.offset(x, y, depth - 1), wallMaterial));
                }
            }
            for (int z = 1; z < depth - 1; z++) {
                // Left and right walls with multiple windows
                if (y >= 2 && y <= height - 1 && (z % 3 == 1)) {
                    blocks.add(new BlockPlacement(start.offset(0, y, z), windowMaterial));
                    blocks.add(new BlockPlacement(start.offset(width - 1, y, z), windowMaterial));
                } else {
                    blocks.add(new BlockPlacement(start.offset(0, y, z), wallMaterial));
                    blocks.add(new BlockPlacement(start.offset(width - 1, y, z), wallMaterial));
                }
            }
        }
        int roofStartHeight = height + 1;
        int roofLayers = Math.max(width, depth) / 2 + 1;
        
        for (int layer = 0; layer < roofLayers; layer++) {
            int currentHeight = roofStartHeight + layer;
            int inset = layer;
            
            for (int x = inset; x < width - inset; x++) {
                for (int z = inset; z < depth - inset; z++) {
                    if (x == inset || x == width - 1 - inset || 
                        z == inset || z == depth - 1 - inset) {
                        blocks.add(new BlockPlacement(start.offset(x, currentHeight, z), roofMaterial));
                    }
                }
            }
            
            if (width - 2 * inset <= 1 || depth - 2 * inset <= 1) {
                break;
            }
        }
        
        return blocks;
    }
    
    private List<BlockPlacement> buildCastle(BlockPos start, int width, int height, int depth) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block stoneMaterial = Blocks.STONE_BRICKS;
        Block wallMaterial = Blocks.COBBLESTONE;
        Block accentMaterial = getMaterial(2); // Use third material for accent
        Block windowMaterial = Blocks.GLASS_PANE;
        
        for (int y = 0; y <= height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == depth - 1);
                    boolean isCorner = (x <= 2 || x >= width - 3) && (z <= 2 || z >= depth - 3);
                    
                    if (y == 0) {
                        // Solid stone floor
                        blocks.add(new BlockPlacement(start.offset(x, y, z), stoneMaterial));
                    } else if (isEdge && !isCorner) {
                        if (x == width / 2 && z == 0 && y <= 3) {
                            if (y >= 1 && y <= 3 && x >= width / 2 - 1 && x <= width / 2 + 1) {
                                blocks.add(new BlockPlacement(start.offset(x, y, 0), Blocks.AIR));
                            }
                        } else if (y % 4 == 2 && !isCorner) {
                            // Arrow slit windows
                            blocks.add(new BlockPlacement(start.offset(x, y, z), windowMaterial));
                        } else {
                            // Thick stone walls
                            blocks.add(new BlockPlacement(start.offset(x, y, z), wallMaterial));
                        }
                    }
                }
            }
        }
        
        int towerHeight = height + 6; // Much taller towers
        int towerSize = 3;
        int[][] corners = {{0, 0}, {width - towerSize, 0}, {0, depth - towerSize}, {width - towerSize, depth - towerSize}};
        
        for (int[] corner : corners) {
            for (int y = 0; y <= towerHeight; y++) {
                for (int dx = 0; dx < towerSize; dx++) {
                    for (int dz = 0; dz < towerSize; dz++) {
                        boolean isTowerEdge = (dx == 0 || dx == towerSize - 1 || dz == 0 || dz == towerSize - 1);
                        
                        if (y == 0 || isTowerEdge) {
                            // Solid base and hollow center
                            blocks.add(new BlockPlacement(start.offset(corner[0] + dx, y, corner[1] + dz), stoneMaterial));
                        }
                        
                        // Windows on towers
                        if (y % 5 == 3 && isTowerEdge && (dx == towerSize / 2 || dz == towerSize / 2)) {
                            blocks.add(new BlockPlacement(start.offset(corner[0] + dx, y, corner[1] + dz), windowMaterial));
                        }
                    }
                }
            }
            for (int dx = 0; dx < towerSize; dx++) {
                for (int dz = 0; dz < towerSize; dz++) {
                    if (dx % 2 == 0 || dz % 2 == 0) {
                        blocks.add(new BlockPlacement(start.offset(corner[0] + dx, towerHeight + 1, corner[1] + dz), stoneMaterial));
                    }
                }
            }
        }
        for (int x = 0; x < width; x += 2) {
            blocks.add(new BlockPlacement(start.offset(x, height + 1, 0), stoneMaterial));
            blocks.add(new BlockPlacement(start.offset(x, height + 2, 0), stoneMaterial));
            blocks.add(new BlockPlacement(start.offset(x, height + 1, depth - 1), stoneMaterial));
            blocks.add(new BlockPlacement(start.offset(x, height + 2, depth - 1), stoneMaterial));
        }
        for (int z = 0; z < depth; z += 2) {
            blocks.add(new BlockPlacement(start.offset(0, height + 1, z), stoneMaterial));
            blocks.add(new BlockPlacement(start.offset(0, height + 2, z), stoneMaterial));
            blocks.add(new BlockPlacement(start.offset(width - 1, height + 1, z), stoneMaterial));
            blocks.add(new BlockPlacement(start.offset(width - 1, height + 2, z), stoneMaterial));
        }
        
        return blocks;
    }
    
    private List<BlockPlacement> buildModernHouse(BlockPos start, int width, int height, int depth) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block wallMaterial = Blocks.QUARTZ_BLOCK;
        Block floorMaterial = Blocks.SMOOTH_STONE;
        Block glassMaterial = Blocks.GLASS;
        Block roofMaterial = Blocks.DARK_OAK_PLANKS;
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), floorMaterial));
            }
        }
        
        // Modern design with lots of glass
        for (int y = 1; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Front - mostly glass
                if (x % 2 == 0 || y > 1) {
                    blocks.add(new BlockPlacement(start.offset(x, y, 0), glassMaterial));
                } else {
                    blocks.add(new BlockPlacement(start.offset(x, y, 0), wallMaterial));
                }
                
                blocks.add(new BlockPlacement(start.offset(x, y, depth - 1), wallMaterial));
            }
            
            for (int z = 1; z < depth - 1; z++) {
                // Side walls with some glass
                if (z % 3 == 1 && y == 2) {
                    blocks.add(new BlockPlacement(start.offset(0, y, z), glassMaterial));
                    blocks.add(new BlockPlacement(start.offset(width - 1, y, z), glassMaterial));
                } else {
                    blocks.add(new BlockPlacement(start.offset(0, y, z), wallMaterial));
                    blocks.add(new BlockPlacement(start.offset(width - 1, y, z), wallMaterial));
                }
            }
        }
        
        // Flat modern roof
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, height, z), roofMaterial));
            }
        }
        
        return blocks;
    }
    
    private List<BlockPlacement> buildBarn(BlockPos start, int width, int height, int depth) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block woodMaterial = Blocks.OAK_PLANKS;
        Block logMaterial = Blocks.OAK_LOG;
        Block roofMaterial = Blocks.SPRUCE_PLANKS;
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), woodMaterial));
            }
        }
        
        for (int y = 1; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isSupport = (x == 0 || x == width - 1 || x == width / 2);
                Block material = isSupport ? logMaterial : woodMaterial;
                
                // Large door opening in front
                if (x >= width / 3 && x <= 2 * width / 3 && y <= 2) {
                    continue; // Skip for large opening
                }
                
                blocks.add(new BlockPlacement(start.offset(x, y, 0), material));
                blocks.add(new BlockPlacement(start.offset(x, y, depth - 1), material));
            }
            
            for (int z = 1; z < depth - 1; z++) {
                blocks.add(new BlockPlacement(start.offset(0, y, z), logMaterial));
                blocks.add(new BlockPlacement(start.offset(width - 1, y, z), logMaterial));
            }
        }
        
        // Tall peaked roof
        int roofPeakHeight = height + width / 2;
        for (int x = 0; x < width; x++) {
            int distFromCenter = Math.abs(x - width / 2);
            int roofY = roofPeakHeight - distFromCenter;
            
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, roofY, z), roofMaterial));
            }
        }
        
        return blocks;
    }
    
    private List<BlockPlacement> buildAdvancedTower(BlockPos start, int width, int height) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block wallMaterial = Blocks.STONE_BRICKS;
        Block accentMaterial = Blocks.CHISELED_STONE_BRICKS;
        Block windowMaterial = Blocks.GLASS_PANE;
        Block roofMaterial = Blocks.DARK_OAK_STAIRS;
        
        // Main tower body
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == width - 1);
                    boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == width - 1);
                    
                    if (y == 0) {
                        blocks.add(new BlockPlacement(start.offset(x, y, z), wallMaterial));
                    } else if (isEdge) {
                        // Windows every few levels
                        if (y % 3 == 2 && !isCorner && (x == width / 2 || z == width / 2)) {
                            blocks.add(new BlockPlacement(start.offset(x, y, z), windowMaterial));
                        } else if (isCorner) {
                            blocks.add(new BlockPlacement(start.offset(x, y, z), accentMaterial));
                        } else {
                            blocks.add(new BlockPlacement(start.offset(x, y, z), wallMaterial));
                        }
                    }
                }
            }
        }
        
        for (int i = 0; i < width / 2 + 1; i++) {
            for (int x = i; x < width - i; x++) {
                for (int z = i; z < width - i; z++) {
                    if (x == i || x == width - 1 - i || z == i || z == width - 1 - i) {
                        blocks.add(new BlockPlacement(start.offset(x, height + i, z), roofMaterial));
                    }
                }
            }
        }
        
        return blocks;
    }

    private Block parseBlock(String blockName) {
        blockName = blockName.toLowerCase().replace(" ", "_");
        if (!blockName.contains(":")) {
            blockName = "minecraft:" + blockName;
        }
        ResourceLocation resourceLocation = new ResourceLocation(blockName);
        Block block = BuiltInRegistries.BLOCK.get(resourceLocation);
        return block != null ? block : Blocks.AIR;
    }
    
    /**
     * Find the actual ground level from a starting position
     * Scans downward to find solid ground, or upward if underground
     */
    private BlockPos findGroundLevel(BlockPos startPos) {
        int maxScanDown = 20; // Scan up to 20 blocks down
        int maxScanUp = 10;   // Scan up to 10 blocks up if we're underground
        
        // First, try scanning downward to find ground
        for (int i = 0; i < maxScanDown; i++) {
            BlockPos checkPos = startPos.below(i);
            BlockPos belowPos = checkPos.below();
            
            if (crafto.level().getBlockState(checkPos).isAir() && 
                isSolidGround(belowPos)) {
                return checkPos; // This is ground level
            }
        }
        
        // Scan upward to find the surface
        for (int i = 1; i < maxScanUp; i++) {
            BlockPos checkPos = startPos.above(i);
            BlockPos belowPos = checkPos.below();
            
            if (crafto.level().getBlockState(checkPos).isAir() && 
                isSolidGround(belowPos)) {
                return checkPos;
            }
        }
        
        // but make sure there's something solid below
        BlockPos fallbackPos = startPos;
        while (!isSolidGround(fallbackPos.below()) && fallbackPos.getY() > -64) {
            fallbackPos = fallbackPos.below();
        }
        
        return fallbackPos;
    }
    
    /**
     * Check if a position has solid ground suitable for building
     */
    private boolean isSolidGround(BlockPos pos) {
        var blockState = crafto.level().getBlockState(pos);
        var block = blockState.getBlock();
        
        // Not solid if it's air or liquid
        if (blockState.isAir() || block == Blocks.WATER || block == Blocks.LAVA) {
            return false;
        }
        
        return blockState.isSolid();
    }
    
    /**
     * Find a suitable building site with flat, clear ground
     * Searches for an area that is:
     * - Relatively flat (max 2 block height difference)
     * - Clear of obstructions (trees, rocks, etc.)
     * - Has enough vertical space for the structure
     */
    private BlockPos findSuitableBuildingSite(BlockPos startPos, int width, int height, int depth) {
        int maxSearchRadius = 10;
        int searchStep = 3; // Small steps to stay nearby
        
        if (isAreaSuitable(startPos, width, height, depth)) {
            return startPos;
        }        // Search in expanding circles
        for (int radius = searchStep; radius < maxSearchRadius; radius += searchStep) {
            for (int angle = 0; angle < 360; angle += 45) { // Check every 45 degrees
                double radians = Math.toRadians(angle);
                int offsetX = (int) (Math.cos(radians) * radius);
                int offsetZ = (int) (Math.sin(radians) * radius);
                
                BlockPos testPos = new BlockPos(
                    startPos.getX() + offsetX,
                    startPos.getY(),
                    startPos.getZ() + offsetZ
                );
                
                BlockPos groundPos = findGroundLevel(testPos);
                if (groundPos != null && isAreaSuitable(groundPos, width, height, depth)) {
                    CraftoMod.LOGGER.info("Found suitable flat ground at {} ({}m away)", groundPos, radius);
                    return groundPos;
                }
            }
        }
        
        CraftoMod.LOGGER.warn("Could not find suitable flat ground within {}m", maxSearchRadius);
        return null;
    }
    
    /**
     * Check if an area is suitable for building
     * - Must be relatively flat (max 2 block height variation)
     * - Must be clear of obstructions above ground
     * - Must have solid ground below
     */
    private boolean isAreaSuitable(BlockPos startPos, int width, int height, int depth) {
        // Sample key points in the build area to check terrain
        int samples = 0;
        int maxSamples = 9; // Check 9 points (corners + center + midpoints)
        int unsuitable = 0;
        
        BlockPos[] checkPoints = {
            startPos,                                    // Front-left corner
            startPos.offset(width - 1, 0, 0),           // Front-right corner
            startPos.offset(0, 0, depth - 1),           // Back-left corner
            startPos.offset(width - 1, 0, depth - 1),   // Back-right corner
            startPos.offset(width / 2, 0, depth / 2),   // Center
            startPos.offset(width / 2, 0, 0),           // Front-center
            startPos.offset(width / 2, 0, depth - 1),   // Back-center
            startPos.offset(0, 0, depth / 2),           // Left-center
            startPos.offset(width - 1, 0, depth / 2)    // Right-center
        };
        
        int minY = startPos.getY();
        int maxY = startPos.getY();
        
        for (BlockPos checkPos : checkPoints) {
            samples++;
            
            if (!isSolidGround(checkPos.below())) {
                unsuitable++;
                continue;
            }
            
            BlockPos actualGround = findGroundLevel(checkPos);
            if (actualGround != null) {
                minY = Math.min(minY, actualGround.getY());
                maxY = Math.max(maxY, actualGround.getY());
            }
            
            for (int y = 1; y <= Math.min(height, 3); y++) {
                BlockPos abovePos = checkPos.above(y);
                var blockState = crafto.level().getBlockState(abovePos);
                
                if (!blockState.isAir()) {
                    Block block = blockState.getBlock();
                    if (block != Blocks.GRASS && block != Blocks.TALL_GRASS && 
                        block != Blocks.FERN && block != Blocks.DEAD_BUSH &&
                        block != Blocks.DANDELION && block != Blocks.POPPY) {
                        unsuitable++;
                        break;
                    }
                }
            }
        }
        
        int heightVariation = maxY - minY;
        if (heightVariation > 2) {
            CraftoMod.LOGGER.debug("Area at {} too uneven ({}m height difference)", startPos, heightVariation);
            return false;
        }
        
        // Area is suitable if less than 30% of samples are problematic
        boolean suitable = unsuitable < (maxSamples * 0.3);
        
        if (!suitable) {
            CraftoMod.LOGGER.debug("Area at {} has too many obstructions ({}/{})", startPos, unsuitable, samples);
        }
        
        return suitable;
    }
    
    /**
     * Try to load structure from NBT template file
     * Returns null if no template found (falls back to procedural generation)
     */
    private List<BlockPlacement> tryLoadFromTemplate(String structureName, BlockPos startPos) {
        if (!(crafto.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        
        var template = StructureTemplateLoader.loadFromNBT(serverLevel, structureName);
        if (template == null) {
            return null;
        }
        
        List<BlockPlacement> blocks = new ArrayList<>();
        for (var templateBlock : template.blocks) {
            BlockPos worldPos = startPos.offset(templateBlock.relativePos);
            Block block = templateBlock.blockState.getBlock();
            blocks.add(new BlockPlacement(worldPos, block));
        }
        
        return blocks;
    }
    
    /**
     * Find the nearest player to build in front of
     */
    private net.minecraft.world.entity.player.Player findNearestPlayer() {
        java.util.List<? extends net.minecraft.world.entity.player.Player> players = crafto.level().players();
        
        if (players.isEmpty()) {
            return null;
        }
        
        net.minecraft.world.entity.player.Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (net.minecraft.world.entity.player.Player player : players) {
            if (!player.isAlive() || player.isRemoved() || player.isSpectator()) {
                continue;
            }
            
            double distance = crafto.distanceTo(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        
        return nearest;
    }
    
}

