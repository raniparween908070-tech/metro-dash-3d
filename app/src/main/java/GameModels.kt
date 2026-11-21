package com.example.data

enum class CurrencyType {
    COINS,
    DIAMONDS
}

enum class PowerUpType(
    val id: String,
    val title: String,
    val description: String,
    val baseDurationSec: Float,
    val perLevelBonusSec: Float,
    val baseUpgradeCostCoins: Int
) {
    MAGNET(
        id = "magnet",
        title = "Coin Magnet",
        description = "Pulls nearby 3D coins across all three lanes straight to your runner.",
        baseDurationSec = 8f,
        perLevelBonusSec = 2f,
        baseUpgradeCostCoins = 400
    ),
    JETPACK(
        id = "jetpack",
        title = "Turbo Jetpack",
        description = "Soars above trains and barriers along an overhead sky-coin trail.",
        baseDurationSec = 6f,
        perLevelBonusSec = 1.5f,
        baseUpgradeCostCoins = 600
    ),
    SNEAKERS(
        id = "sneakers",
        title = "Super Sneakers",
        description = "Boosts jump height to vault over trains and tall railway gates.",
        baseDurationSec = 10f,
        perLevelBonusSec = 2f,
        baseUpgradeCostCoins = 450
    ),
    SCORE_2X(
        id = "score_2x",
        title = "2X Multiplier",
        description = "Doubles all distance and pickup score points while active.",
        baseDurationSec = 12f,
        perLevelBonusSec = 2.5f,
        baseUpgradeCostCoins = 500
    ),
    SHIELD(
        id = "shield",
        title = "Energy Shield",
        description = "Absorbs one collision impact and blasts nearby obstacles clear.",
        baseDurationSec = 14f,
        perLevelBonusSec = 3f,
        baseUpgradeCostCoins = 550
    ),
    HOVERBOARD(
        id = "hoverboard",
        title = "Hoverboard",
        description = "Activate manually for high-speed rail surfing and crash protection.",
        baseDurationSec = 15f,
        perLevelBonusSec = 2.5f,
        baseUpgradeCostCoins = 650
    );

    fun durationForLevel(level: Int): Float =
        baseDurationSec + (level.coerceIn(1, 5) - 1) * perLevelBonusSec

    fun upgradeCostForNextLevel(currentLevel: Int): Int? =
        if (currentLevel >= 5) null else baseUpgradeCostCoins * currentLevel * currentLevel
}

enum class GraphicsQuality(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

enum class MetroBiome(
    val title: String,
    val skyTop: FloatArray,
    val skyHorizon: FloatArray,
    val groundColor: FloatArray,
    val sleeperColor: FloatArray,
    val railGlow: FloatArray,
    val buildingColor: FloatArray,
    val trainBody: FloatArray,
    val trainAccent: FloatArray,
    val fogColor: FloatArray
) {
    NEON_DOWNTOWN(
        title = "Neon Downtown",
        skyTop = floatArrayOf(0.03f, 0.06f, 0.16f),
        skyHorizon = floatArrayOf(0.10f, 0.22f, 0.42f),
        groundColor = floatArrayOf(0.11f, 0.14f, 0.22f),
        sleeperColor = floatArrayOf(0.24f, 0.20f, 0.18f),
        railGlow = floatArrayOf(0.00f, 0.90f, 1.00f),
        buildingColor = floatArrayOf(0.13f, 0.18f, 0.34f),
        trainBody = floatArrayOf(0.14f, 0.56f, 0.85f),
        trainAccent = floatArrayOf(1.00f, 0.78f, 0.08f),
        fogColor = floatArrayOf(0.08f, 0.16f, 0.32f)
    ),
    SUNSET_VIADUCT(
        title = "Sunset Viaduct",
        skyTop = floatArrayOf(0.18f, 0.07f, 0.24f),
        skyHorizon = floatArrayOf(0.86f, 0.36f, 0.22f),
        groundColor = floatArrayOf(0.18f, 0.14f, 0.20f),
        sleeperColor = floatArrayOf(0.32f, 0.22f, 0.18f),
        railGlow = floatArrayOf(1.00f, 0.60f, 0.18f),
        buildingColor = floatArrayOf(0.26f, 0.16f, 0.28f),
        trainBody = floatArrayOf(0.92f, 0.32f, 0.22f),
        trainAccent = floatArrayOf(1.00f, 0.92f, 0.45f),
        fogColor = floatArrayOf(0.55f, 0.25f, 0.24f)
    ),
    MIDNIGHT_SUBWAY(
        title = "Midnight Tunnel",
        skyTop = floatArrayOf(0.02f, 0.03f, 0.07f),
        skyHorizon = floatArrayOf(0.05f, 0.10f, 0.18f),
        groundColor = floatArrayOf(0.09f, 0.10f, 0.15f),
        sleeperColor = floatArrayOf(0.20f, 0.18f, 0.20f),
        railGlow = floatArrayOf(0.22f, 0.96f, 0.65f),
        buildingColor = floatArrayOf(0.08f, 0.11f, 0.19f),
        trainBody = floatArrayOf(0.16f, 0.72f, 0.56f),
        trainAccent = floatArrayOf(0.85f, 0.98f, 1.00f),
        fogColor = floatArrayOf(0.04f, 0.08f, 0.15f)
    ),
    HARBOR_BRIDGE(
        title = "Harbor Bridge",
        skyTop = floatArrayOf(0.06f, 0.22f, 0.45f),
        skyHorizon = floatArrayOf(0.26f, 0.62f, 0.80f),
        groundColor = floatArrayOf(0.16f, 0.22f, 0.28f),
        sleeperColor = floatArrayOf(0.28f, 0.26f, 0.24f),
        railGlow = floatArrayOf(0.18f, 0.85f, 0.98f),
        buildingColor = floatArrayOf(0.20f, 0.30f, 0.44f),
        trainBody = floatArrayOf(0.95f, 0.72f, 0.12f),
        trainAccent = floatArrayOf(0.12f, 0.22f, 0.38f),
        fogColor = floatArrayOf(0.20f, 0.46f, 0.64f)
    ),
    SAKURA_OVERPASS(
        title = "Sakura Overpass",
        skyTop = floatArrayOf(0.15f, 0.10f, 0.32f),
        skyHorizon = floatArrayOf(0.88f, 0.50f, 0.68f),
        groundColor = floatArrayOf(0.16f, 0.15f, 0.24f),
        sleeperColor = floatArrayOf(0.30f, 0.22f, 0.26f),
        railGlow = floatArrayOf(1.00f, 0.48f, 0.78f),
        buildingColor = floatArrayOf(0.24f, 0.19f, 0.36f),
        trainBody = floatArrayOf(0.95f, 0.44f, 0.68f),
        trainAccent = floatArrayOf(1.00f, 0.94f, 0.98f),
        fogColor = floatArrayOf(0.54f, 0.32f, 0.48f)
    ),
    CYBER_YARD(
        title = "Cyber Yard",
        skyTop = floatArrayOf(0.05f, 0.02f, 0.14f),
        skyHorizon = floatArrayOf(0.36f, 0.08f, 0.52f),
        groundColor = floatArrayOf(0.10f, 0.08f, 0.18f),
        sleeperColor = floatArrayOf(0.20f, 0.15f, 0.30f),
        railGlow = floatArrayOf(0.85f, 0.26f, 1.00f),
        buildingColor = floatArrayOf(0.15f, 0.10f, 0.28f),
        trainBody = floatArrayOf(0.56f, 0.22f, 0.92f),
        trainAccent = floatArrayOf(0.00f, 0.95f, 0.90f),
        fogColor = floatArrayOf(0.22f, 0.06f, 0.34f)
    ),
    ALPINE_FROST(
        title = "Alpine Express",
        skyTop = floatArrayOf(0.08f, 0.26f, 0.44f),
        skyHorizon = floatArrayOf(0.58f, 0.80f, 0.92f),
        groundColor = floatArrayOf(0.22f, 0.28f, 0.36f),
        sleeperColor = floatArrayOf(0.32f, 0.36f, 0.42f),
        railGlow = floatArrayOf(0.45f, 0.92f, 1.00f),
        buildingColor = floatArrayOf(0.30f, 0.40f, 0.54f),
        trainBody = floatArrayOf(0.24f, 0.68f, 0.95f),
        trainAccent = floatArrayOf(0.98f, 0.98f, 1.00f),
        fogColor = floatArrayOf(0.40f, 0.62f, 0.78f)
    ),
    GOLDEN_TERMINAL(
        title = "Golden Terminal",
        skyTop = floatArrayOf(0.12f, 0.08f, 0.04f),
        skyHorizon = floatArrayOf(0.68f, 0.48f, 0.14f),
        groundColor = floatArrayOf(0.16f, 0.14f, 0.12f),
        sleeperColor = floatArrayOf(0.32f, 0.26f, 0.18f),
        railGlow = floatArrayOf(1.00f, 0.82f, 0.20f),
        buildingColor = floatArrayOf(0.28f, 0.22f, 0.16f),
        trainBody = floatArrayOf(0.96f, 0.74f, 0.15f),
        trainAccent = floatArrayOf(0.14f, 0.14f, 0.18f),
        fogColor = floatArrayOf(0.42f, 0.30f, 0.12f)
    );
}

data class StageConfig(
    val stageNumber: Int,
    val targetDistanceMeters: Int = 5000,
    val biome: MetroBiome,
    val baseSpeed: Float,
    val maxSpeed: Float,
    val obstacleDensity: Float,
    val movingTrainRatio: Float,
    val tunnelProbability: Float,
    val bridgeProbability: Float,
    val rewardCoins: Int,
    val rewardDiamonds: Int
)

object StageGenerator {
    const val TOTAL_STAGES = 1000
    const val STAGE_DISTANCE_METERS = 5000
    const val TOTAL_CAMPAIGN_METERS = TOTAL_STAGES * STAGE_DISTANCE_METERS // 5,000,000 meters

    fun configForStage(stage: Int): StageConfig {
        val s = stage.coerceIn(1, TOTAL_STAGES)
        val biome = MetroBiome.entries[((s - 1) / 5) % MetroBiome.entries.size]
        val progress = (s - 1) / (TOTAL_STAGES - 1f)
        // Base speed ramps smoothly from 18 m/s (Stage 1) up to 34 m/s (Stage 1000)
        val baseSpeed = 18.5f + progress * 15.5f
        val maxSpeed = baseSpeed + 10.0f
        val obstacleDensity = (0.48f + progress * 0.44f).coerceIn(0.45f, 0.94f)
        val movingTrainRatio = (0.28f + progress * 0.42f).coerceIn(0.25f, 0.72f)
        val tunnelProbability = if (biome == MetroBiome.MIDNIGHT_SUBWAY) 0.55f else (0.14f + (s % 4) * 0.05f)
        val bridgeProbability = if (biome == MetroBiome.HARBOR_BRIDGE) 0.55f else (0.16f + (s % 3) * 0.05f)
        val rewardCoins = 250 + ((s - 1) % 25) * 20 + (s / 20) * 15
        val rewardDiamonds = 2 + (if (s % 5 == 0) 3 else 0) + (if (s % 25 == 0) 5 else 0)
        return StageConfig(
            stageNumber = s,
            targetDistanceMeters = STAGE_DISTANCE_METERS,
            biome = biome,
            baseSpeed = baseSpeed,
            maxSpeed = maxSpeed,
            obstacleDensity = obstacleDensity,
            movingTrainRatio = movingTrainRatio,
            tunnelProbability = tunnelProbability,
            bridgeProbability = bridgeProbability,
            rewardCoins = rewardCoins,
            rewardDiamonds = rewardDiamonds
        )
    }
}

data class CharacterDef(
    val id: String,
    val name: String,
    val tagline: String,
    val currency: CurrencyType,
    val price: Int,
    val primaryColor: FloatArray,
    val secondaryColor: FloatArray,
    val visorColor: FloatArray,
    val skinColor: FloatArray,
    val hasHelmet: Boolean
)

data class OutfitDef(
    val id: String,
    val characterId: String,
    val name: String,
    val currency: CurrencyType,
    val price: Int,
    val primaryColor: FloatArray,
    val secondaryColor: FloatArray,
    val accentColor: FloatArray
)

data class HoverboardDef(
    val id: String,
    val name: String,
    val tagline: String,
    val currency: CurrencyType,
    val price: Int,
    val deckColor: FloatArray,
    val glowColor: FloatArray,
    val speedMultiplier: Float
)

data class DailyRewardDay(
    val dayIndex: Int, // 1..7
    val coins: Int,
    val diamonds: Int,
    val hoverboards: Int
)

data class CurrencyPackDef(
    val id: String,
    val title: String,
    val coinsGranted: Int,
    val diamondCost: Int
)

enum class MissionType(val label: String) {
    DISTANCE("Run Distance"),
    COINS("Collect Coins"),
    JUMPS("Jump Obstacles"),
    STAGE_CLEAR("Complete Stages"),
    POWERUPS("Use Power-Ups")
}

object GameCatalog {
    val characters = listOf(
        CharacterDef(
            id = "bolt",
            name = "Bolt",
            tagline = "Electric Courier of Sector 1",
            currency = CurrencyType.COINS,
            price = 0,
            primaryColor = floatArrayOf(0.05f, 0.80f, 0.96f),
            secondaryColor = floatArrayOf(0.12f, 0.16f, 0.28f),
            visorColor = floatArrayOf(1.00f, 0.82f, 0.12f),
            skinColor = floatArrayOf(0.95f, 0.78f, 0.64f),
            hasHelmet = false
        ),
        CharacterDef(
            id = "nova",
            name = "Nova",
            tagline = "Solar Roof-Jumper",
            currency = CurrencyType.COINS,
            price = 1500,
            primaryColor = floatArrayOf(1.00f, 0.45f, 0.12f),
            secondaryColor = floatArrayOf(0.18f, 0.16f, 0.24f),
            visorColor = floatArrayOf(0.15f, 0.95f, 0.95f),
            skinColor = floatArrayOf(0.84f, 0.62f, 0.48f),
            hasHelmet = true
        ),
        CharacterDef(
            id = "vex",
            name = "Vex",
            tagline = "Neon Night Phantom",
            currency = CurrencyType.COINS,
            price = 3500,
            primaryColor = floatArrayOf(0.68f, 0.24f, 0.96f),
            secondaryColor = floatArrayOf(0.10f, 0.10f, 0.18f),
            visorColor = floatArrayOf(0.20f, 1.00f, 0.65f),
            skinColor = floatArrayOf(0.92f, 0.75f, 0.66f),
            hasHelmet = true
        ),
        CharacterDef(
            id = "kira",
            name = "Kira",
            tagline = "Emerald Rail Sprinter",
            currency = CurrencyType.DIAMONDS,
            price = 35,
            primaryColor = floatArrayOf(0.08f, 0.86f, 0.56f),
            secondaryColor = floatArrayOf(0.10f, 0.20f, 0.24f),
            visorColor = floatArrayOf(1.00f, 0.30f, 0.65f),
            skinColor = floatArrayOf(0.76f, 0.54f, 0.40f),
            hasHelmet = false
        ),
        CharacterDef(
            id = "titan",
            name = "Titan",
            tagline = "Crimson Armor Vanguard",
            currency = CurrencyType.DIAMONDS,
            price = 75,
            primaryColor = floatArrayOf(0.92f, 0.18f, 0.28f),
            secondaryColor = floatArrayOf(0.22f, 0.24f, 0.30f),
            visorColor = floatArrayOf(0.00f, 0.95f, 1.00f),
            skinColor = floatArrayOf(0.86f, 0.68f, 0.55f),
            hasHelmet = true
        ),
        CharacterDef(
            id = "aether",
            name = "Aether",
            tagline = "Golden Express Legend",
            currency = CurrencyType.DIAMONDS,
            price = 140,
            primaryColor = floatArrayOf(1.00f, 0.78f, 0.12f),
            secondaryColor = floatArrayOf(0.14f, 0.18f, 0.32f),
            visorColor = floatArrayOf(0.95f, 0.98f, 1.00f),
            skinColor = floatArrayOf(0.94f, 0.77f, 0.65f),
            hasHelmet = true
        )
    )

    val outfits = listOf(
        OutfitDef(
            id = "default_classic",
            characterId = "bolt",
            name = "Street Cyan",
            currency = CurrencyType.COINS,
            price = 0,
            primaryColor = floatArrayOf(0.05f, 0.80f, 0.96f),
            secondaryColor = floatArrayOf(0.12f, 0.16f, 0.28f),
            accentColor = floatArrayOf(1.00f, 0.82f, 0.12f)
        ),
        OutfitDef(
            id = "bolt_voltage",
            characterId = "bolt",
            name = "Voltage Gold",
            currency = CurrencyType.COINS,
            price = 1200,
            primaryColor = floatArrayOf(1.00f, 0.76f, 0.08f),
            secondaryColor = floatArrayOf(0.08f, 0.14f, 0.28f),
            accentColor = floatArrayOf(0.00f, 0.95f, 1.00f)
        ),
        OutfitDef(
            id = "nova_inferno",
            characterId = "nova",
            name = "Magma Core",
            currency = CurrencyType.COINS,
            price = 1800,
            primaryColor = floatArrayOf(0.95f, 0.22f, 0.18f),
            secondaryColor = floatArrayOf(0.16f, 0.12f, 0.18f),
            accentColor = floatArrayOf(1.00f, 0.84f, 0.20f)
        ),
        OutfitDef(
            id = "vex_holo",
            characterId = "vex",
            name = "Holo Prism",
            currency = CurrencyType.DIAMONDS,
            price = 25,
            primaryColor = floatArrayOf(0.18f, 0.92f, 0.98f),
            secondaryColor = floatArrayOf(0.32f, 0.10f, 0.50f),
            accentColor = floatArrayOf(1.00f, 0.32f, 0.84f)
        ),
        OutfitDef(
            id = "kira_jade",
            characterId = "kira",
            name = "Jade Matrix",
            currency = CurrencyType.COINS,
            price = 2400,
            primaryColor = floatArrayOf(0.05f, 0.95f, 0.65f),
            secondaryColor = floatArrayOf(0.06f, 0.16f, 0.14f),
            accentColor = floatArrayOf(0.98f, 0.98f, 1.00f)
        ),
        OutfitDef(
            id = "titan_obsidian",
            characterId = "titan",
            name = "Obsidian Mech",
            currency = CurrencyType.DIAMONDS,
            price = 40,
            primaryColor = floatArrayOf(0.22f, 0.24f, 0.30f),
            secondaryColor = floatArrayOf(0.06f, 0.07f, 0.10f),
            accentColor = floatArrayOf(1.00f, 0.28f, 0.28f)
        ),
        OutfitDef(
            id = "aether_celestial",
            characterId = "aether",
            name = "Celestial Crown",
            currency = CurrencyType.DIAMONDS,
            price = 60,
            primaryColor = floatArrayOf(0.96f, 0.96f, 1.00f),
            secondaryColor = floatArrayOf(0.16f, 0.22f, 0.44f),
            accentColor = floatArrayOf(1.00f, 0.82f, 0.18f)
        )
    )

    val hoverboards = listOf(
        HoverboardDef(
            id = "metro_cruiser",
            name = "Metro Cruiser",
            tagline = "Balanced neon rail board",
            currency = CurrencyType.COINS,
            price = 0,
            deckColor = floatArrayOf(0.08f, 0.78f, 0.96f),
            glowColor = floatArrayOf(0.00f, 0.95f, 1.00f),
            speedMultiplier = 1.05f
        ),
        HoverboardDef(
            id = "ion_blade",
            name = "Ion Blade",
            tagline = "Sharp aero-fin rail glider",
            currency = CurrencyType.COINS,
            price = 1400,
            deckColor = floatArrayOf(0.20f, 0.95f, 0.62f),
            glowColor = floatArrayOf(0.40f, 1.00f, 0.80f),
            speedMultiplier = 1.12f
        ),
        HoverboardDef(
            id = "magma_surf",
            name = "Magma Surf",
            tagline = "High-heat twin-thruster deck",
            currency = CurrencyType.COINS,
            price = 2800,
            deckColor = floatArrayOf(0.98f, 0.36f, 0.14f),
            glowColor = floatArrayOf(1.00f, 0.80f, 0.20f),
            speedMultiplier = 1.16f
        ),
        HoverboardDef(
            id = "prism_wing",
            name = "Prism Wing",
            tagline = "Crystal plasma stabilizer",
            currency = CurrencyType.DIAMONDS,
            price = 30,
            deckColor = floatArrayOf(0.84f, 0.24f, 0.98f),
            glowColor = floatArrayOf(0.25f, 0.95f, 1.00f),
            speedMultiplier = 1.20f
        ),
        HoverboardDef(
            id = "hyper_nova",
            name = "Hyper Nova",
            tagline = "Apex golden bullet deck",
            currency = CurrencyType.DIAMONDS,
            price = 65,
            deckColor = floatArrayOf(1.00f, 0.80f, 0.15f),
            glowColor = floatArrayOf(1.00f, 0.96f, 0.70f),
            speedMultiplier = 1.25f
        )
    )

    val dailyRewards = listOf(
        DailyRewardDay(1, coins = 300, diamonds = 2, hoverboards = 1),
        DailyRewardDay(2, coins = 500, diamonds = 3, hoverboards = 1),
        DailyRewardDay(3, coins = 800, diamonds = 5, hoverboards = 2),
        DailyRewardDay(4, coins = 1100, diamonds = 6, hoverboards = 2),
        DailyRewardDay(5, coins = 1500, diamonds = 8, hoverboards = 3),
        DailyRewardDay(6, coins = 2000, diamonds = 12, hoverboards = 3),
        DailyRewardDay(7, coins = 3500, diamonds = 25, hoverboards = 5)
    )

    val coinPacks = listOf(
        CurrencyPackDef("pack_small", "Starter Coin Vault", coinsGranted = 1000, diamondCost = 10),
        CurrencyPackDef("pack_medium", "Express Coin Crate", coinsGranted = 2800, diamondCost = 25),
        CurrencyPackDef("pack_mega", "Central Bank Vault", coinsGranted = 7500, diamondCost = 60)
    )

    fun characterById(id: String): CharacterDef =
        characters.firstOrNull { it.id == id } ?: characters.first()

    fun outfitById(id: String): OutfitDef? =
        outfits.firstOrNull { it.id == id }

    fun hoverboardById(id: String): HoverboardDef =
        hoverboards.firstOrNull { it.id == id } ?: hoverboards.first()
}
