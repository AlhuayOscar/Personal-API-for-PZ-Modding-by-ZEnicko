// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.json.JSONObject;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.skills.PerkFactory;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

@UsedFromLua
public final class Translator {
    private static List<Language> availableLanguage;
    public static boolean debug;
    private static FileWriter debugFile;
    private static boolean debugErrors;
    private static final Set<String> debugItemEvolvedRecipeName = new HashSet<>();
    private static final Set<String> debugItem = new HashSet<>();
    private static final Set<String> debugMultiStageBuild = new HashSet<>();
    private static final Set<String> debugRecipe = new HashSet<>();
    private static final Set<String> debugRecipeGroups = new HashSet<>();
    private static final Map<String, String> moodles = new HashMap<>();
    private static final Map<String, String> ui = new HashMap<>();
    private static final Map<String, String> survivalGuide = new HashMap<>();
    private static final Map<String, String> contextMenu = new HashMap<>();
    private static final Map<String, String> farming = new HashMap<>();
    private static final Map<String, String> recipe = new LinkedHashMap<>();
    private static final Map<String, String> recipeGroups = new HashMap<>();
    private static final Map<String, String> igui = new HashMap<>();
    private static final Map<String, String> sandbox = new HashMap<>();
    private static final Map<String, String> tooltip = new HashMap<>();
    private static final Map<String, String> challenge = new HashMap<>();
    private static final Set<String> missing = new HashSet<>();
    private static ArrayList<String> azertyLanguages;
    private static final Map<String, String> stash = new HashMap<>();
    private static final Map<String, String> moveables = new HashMap<>();
    private static final Map<String, String> makeup = new HashMap<>();
    private static final Map<String, String> gameSound = new HashMap<>();
    private static final Map<String, String> dynamicRadio = new HashMap<>();
    private static final Map<String, String> items = new HashMap<>();
    private static final Map<String, String> itemName = new HashMap<>();
    private static final Map<String, String> itemEvolvedRecipeName = new HashMap<>();
    private static final Map<String, String> recordedMedia = new HashMap<>();
    private static final Map<String, String> recordedMedia_EN = new HashMap<>();
    private static final Map<String, String> survivorNames = new HashMap<>();
    private static final Map<String, String> attributes = new HashMap<>();
    private static final Map<String, String> fluids = new HashMap<>();
    private static final Map<String, String> entity = new HashMap<>();
    private static final Map<String, String> mapLabel = new HashMap<>();
    private static final Map<String, String> printMedia = new HashMap<>();
    private static final Map<String, String> printText = new HashMap<>();
    private static final Map<String, String> radioData = new HashMap<>();
    private static final Map<String, String> bodyParts = new HashMap<>();
    private static final Map<String, String> brReplacements = new HashMap<>();
    private static final Map<String, String> tempMap = new HashMap<>();
    public static final Map<String, Map<String, String>> BY_NAME = new LinkedHashMap<String, Map<String, String>>() {
        {
            this.put("Tooltip", Translator.tooltip);
            this.put("IG_UI", Translator.igui);
            this.put("Recipes", Translator.recipe);
            this.put("RecipeGroups", Translator.recipeGroups);
            this.put("Farming", Translator.farming);
            this.put("ContextMenu", Translator.contextMenu);
            this.put("SurvivalGuide", Translator.survivalGuide);
            this.put("UI", Translator.ui);
            this.put("Items", Translator.items);
            this.put("ItemName", Translator.itemName);
            this.put("Moodles", Translator.moodles);
            this.put("Sandbox", Translator.sandbox);
            this.put("Challenge", Translator.challenge);
            this.put("Stash", Translator.stash);
            this.put("Moveables", Translator.moveables);
            this.put("MakeUp", Translator.makeup);
            this.put("GameSound", Translator.gameSound);
            this.put("DynamicRadio", Translator.dynamicRadio);
            this.put("EvolvedRecipeName", Translator.itemEvolvedRecipeName);
            this.put("Recorded_Media", Translator.recordedMedia);
            this.put("SurvivorNames", Translator.survivorNames);
            this.put("Attributes", Translator.attributes);
            this.put("Fluids", Translator.fluids);
            this.put("Print_Media", Translator.printMedia);
            this.put("Print_Text", Translator.printText);
            this.put("Entity", Translator.entity);
            this.put("RadioData", Translator.radioData);
            this.put("BodyParts", Translator.bodyParts);
            this.put("MapLabel", Translator.mapLabel);
        }
    };
    public static Language language;

    public static void loadFiles() {
        language = null;
        availableLanguage = null;
        File file = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "translationProblems.txt");
        if (debug) {
            try {
                if (debugFile != null) {
                    debugFile.close();
                }

                debugFile = new FileWriter(file);
            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }

        moodles.clear();
        ui.clear();
        survivalGuide.clear();
        items.clear();
        itemName.clear();
        contextMenu.clear();
        farming.clear();
        recipe.clear();
        recipeGroups.clear();
        igui.clear();
        sandbox.clear();
        tooltip.clear();
        challenge.clear();
        missing.clear();
        stash.clear();
        moveables.clear();
        makeup.clear();
        gameSound.clear();
        dynamicRadio.clear();
        itemEvolvedRecipeName.clear();
        recordedMedia.clear();
        survivorNames.clear();
        attributes.clear();
        fluids.clear();
        printMedia.clear();
        printText.clear();
        radioData.clear();
        bodyParts.clear();
        mapLabel.clear();
        DebugType.Translation.println("translator: language is " + getLanguage());
        debugErrors = false;
        BY_NAME.forEach((name, map) -> forLanguageStack(l -> {
            tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), name, (Map<String, String>)map, l);
            tryFillMapFromMods(name, (Map<String, String>)map, l);
        }));
        tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), "Recorded_Media", recordedMedia_EN, getDefaultLanguage());
        tryFillMapFromMods("Recorded_Media", recordedMedia_EN, getDefaultLanguage());
        if (debug) {
            if (debugErrors) {
                DebugType.Translation.trace("translator: errors detected, please see " + file.getAbsolutePath());
            }

            debugItemEvolvedRecipeName.clear();
            debugItem.clear();
            debugMultiStageBuild.clear();
            debugRecipe.clear();
            debugRecipeGroups.clear();
        }

        PerkFactory.initTranslations();
    }

    public static void forLanguageStack(Consumer<Language> consumer) {
        Set<Language> bases = new LinkedHashSet<>();
        Language language = getLanguage();

        while (language != null && bases.add(language)) {
            language = Languages.instance.getByName(language.base());
        }

        bases.add(getDefaultLanguage());
        Language[] languages = bases.toArray(new Language[0]);

        for (int i = languages.length - 1; i >= 0; i--) {
            consumer.accept(languages[i]);
        }
    }

    private static void tryFillMapFromFile(String rootDir, String fileName, Map<String, String> map, Language language) {
        File file = new File("%s/media/lua/shared/Translate/%s/%s.json".formatted(rootDir, language, fileName));
        if (file.exists()) {
            try {
                String content = Files.readString(file.toPath());
                new JSONObject(content).toMap().forEach((k, v) -> map.put(k, ((String)v).replaceAll("%", "%%").replaceAll("%%(\\d+)", "%$1\\$s")));
            } catch (IOException var6) {
                throw new RuntimeException(var6);
            }
        }
    }

    private static void tryFillMapFromMods(String fileName, Map<String, String> map, Language language) {
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();

        for (int n = 0; n < modIDs.size(); n++) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modIDs.get(n));
            if (mod != null) {
                String modDir = mod.getCommonDir();
                if (modDir != null) {
                    tryFillMapFromFile(modDir, fileName, map, language);
                }

                modDir = mod.getVersionDir();
                if (modDir != null) {
                    tryFillMapFromFile(modDir, fileName, map, language);
                }
            }
        }
    }

    public static void readMapTranslation(ChooseGameInfo.Map map, String dir) {
        tempMap.clear();
        forLanguageStack(lang -> {
            tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), dir, tempMap, lang);
            tryFillMapFromMods(dir, tempMap, lang);
        });
        map.setTitle(tempMap.getOrDefault("title", map.getTitle()));
        map.setDescription(tempMap.getOrDefault("description", map.getDescription()));
    }

    public static void readModTranslation(ChooseGameInfo.Mod mod) {
        tempMap.clear();
        forLanguageStack(lang -> {
            String modDir = mod.getCommonDir();
            if (modDir != null) {
                tryFillMapFromFile(modDir, "Mod", tempMap, lang);
            }

            modDir = mod.getVersionDir();
            if (modDir != null) {
                tryFillMapFromFile(modDir, "Mod", tempMap, lang);
            }
        });
        mod.setName(tempMap.getOrDefault("name", mod.getName()));
        mod.setDescription(tempMap.getOrDefault("description", mod.getDescription()));
    }

    /**
     * Return the translated text for the selected language
     *  If we don't fnid any translation for the selected language, we return the default text (in English)
     */
    public static String getText(String desc) {
        return getTextInternal(desc, false);
    }

    public static String getTextOrNull(String desc) {
        return getTextInternal(desc, true);
    }

    private static String getTextInternal(String desc, boolean nullOK) {
        if (ui == null) {
            loadFiles();
        }

        String result = null;
        if (desc.startsWith("UI_")) {
            result = ui.get(desc);
        } else if (desc.startsWith("Moodles_")) {
            result = moodles.get(desc);
        } else if (desc.startsWith("SurvivalGuide_")) {
            result = survivalGuide.get(desc);
        } else if (desc.startsWith("Farming_")) {
            result = farming.get(desc);
        } else if (desc.startsWith("IGUI_")) {
            result = igui.get(desc);
        } else if (desc.startsWith("ContextMenu_")) {
            result = contextMenu.get(desc);
        } else if (desc.startsWith("GameSound_")) {
            result = gameSound.get(desc);
        } else if (desc.startsWith("Sandbox_")) {
            result = sandbox.get(desc);
        } else if (desc.startsWith("Tooltip_")) {
            result = tooltip.get(desc);
        } else if (desc.startsWith("Challenge_")) {
            result = challenge.get(desc);
        } else if (desc.startsWith("MakeUp")) {
            result = makeup.get(desc);
        } else if (desc.startsWith("Stash_")) {
            result = stash.get(desc);
        } else if (desc.startsWith("RM_")) {
            result = recordedMedia.get(desc);
        } else if (desc.startsWith("SurvivorName_")) {
            result = survivorNames.get(desc);
        } else if (desc.startsWith("SurvivorSurname_")) {
            result = survivorNames.get(desc);
        } else if (desc.startsWith("Attributes_")) {
            result = attributes.get(desc);
        } else if (desc.startsWith("Fluid_")) {
            result = fluids.get(desc);
        } else if (desc.startsWith("Print_Media_")) {
            result = printMedia.get(desc);
        } else if (desc.startsWith("Print_Text_")) {
            result = printText.get(desc);
        } else if (desc.startsWith("EC_")) {
            result = entity.get(desc);
        } else if (desc.startsWith("RD_")) {
            result = radioData.get(desc);
        } else if (desc.startsWith("BODYPART_")) {
            result = bodyParts.get(desc);
        } else if (desc.startsWith("MapLabel_")) {
            result = mapLabel.get(desc);
        }

        String dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" : null;
        if (result == null) {
            if (nullOK) {
                return null;
            }

            if (!missing.contains(desc)) {
                if (Core.debug) {
                    DebugType.Translation.error("ERROR: Missing translation \"" + desc + "\"");
                }

                if (debug) {
                    debugwrite("ERROR: Missing translation \"" + desc + "\"\r\n");
                }

                missing.add(desc);
            }

            result = desc;
            dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" : null;
        }

        if (!result.contains("<br>") && !result.contains("<BR>")) {
            return dbg == null ? result : dbg + result;
        } else {
            return brReplacements.computeIfAbsent(result, s -> s.replaceAll("<br>", "\n").replaceAll("<BR>", "\n"));
        }
    }

    public static String getText(String desc, Object... args) {
        return getText(desc).formatted(fixupArgs(args));
    }

    public static String getTextOrNull(String desc, Object... args) {
        String text = getTextOrNull(desc);
        return text == null ? null : text.formatted(fixupArgs(args));
    }

    private static Object[] fixupArgs(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                args[i] = "";
            } else if (args[i] instanceof Double boxedDouble) {
                double d = boxedDouble;
                args[i] = d == (long)d ? Long.toString((long)d) : args[i].toString();
            }
        }

        return args;
    }

    public static void setLanguage(Language newlanguage) {
        if (newlanguage == null) {
            newlanguage = getDefaultLanguage();
        }

        language = newlanguage;
    }

    public static void setLanguage(int languageId) {
        setLanguage(Languages.instance.getLanguages().get(languageId));
    }

    public static Language getLanguage() {
        if (language == null) {
            String languageName = Core.getInstance().getOptionLanguageName();
            if (!StringUtils.isNullOrWhitespace(languageName)) {
                language = Languages.instance.getByName(languageName);
            }
        }

        if (language == null) {
            language = Languages.instance.getByName(System.getProperty("user.language").toUpperCase());
        }

        if (language == null) {
            language = getDefaultLanguage();
        }

        return language;
    }

    public static List<Language> getAvailableLanguage() {
        if (availableLanguage == null) {
            availableLanguage = Languages.instance.getLanguages();
        }

        return availableLanguage;
    }

    public static String getDisplayItemName(String trim) {
        String result = items.get(trim.replaceAll(" ", "_").replaceAll("-", "_"));
        return result == null ? trim : result;
    }

    public static String getItemNameFromFullType(String fullType) {
        if (!fullType.contains(".")) {
            throw new IllegalArgumentException("fullType must contain \".\" i.e. module.type");
        } else {
            String name = itemName.get(fullType);
            if (name == null) {
                if (debug && getLanguage() != getDefaultLanguage() && !debugItem.contains(fullType)) {
                    debugItem.add(fullType);
                }

                Item scriptItem = ScriptManager.instance.getItem(fullType);
                if (scriptItem == null) {
                    name = fullType;
                } else {
                    name = scriptItem.getDisplayName();
                }

                itemName.put(fullType, name);
            }

            return name;
        }
    }

    public static void setDefaultItemEvolvedRecipeName(String fullType, String english) {
        if (getLanguage() == getDefaultLanguage()) {
            if (!fullType.contains(".")) {
                throw new IllegalArgumentException("fullType must contain \".\" i.e. module.type");
            } else if (!itemEvolvedRecipeName.containsKey(fullType)) {
                itemEvolvedRecipeName.put(fullType, english);
            }
        }
    }

    public static String getItemEvolvedRecipeName(String fullType) {
        if (!fullType.contains(".")) {
            throw new IllegalArgumentException("fullType must contain \".\" i.e. module.type");
        } else {
            String name = itemEvolvedRecipeName.get(fullType);
            if (name == null) {
                if (debug && getLanguage() != getDefaultLanguage() && !debugItemEvolvedRecipeName.contains(fullType)) {
                    debugItemEvolvedRecipeName.add(fullType);
                }

                Item scriptItem = ScriptManager.instance.getItem(fullType);
                if (scriptItem == null) {
                    name = fullType;
                } else {
                    name = scriptItem.getDisplayName();
                }

                itemEvolvedRecipeName.put(fullType, name);
            }

            return name;
        }
    }

    public static String getMoveableDisplayName(String name) {
        String replaced = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
        String result = moveables.get(replaced);
        if (result == null) {
            return Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" + name : name;
        } else {
            return Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" + result : result;
        }
    }

    public static String getMoveableDisplayNameOrNull(String name) {
        String replaced = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
        String result = moveables.get(replaced);
        if (result == null) {
            return null;
        } else {
            return Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" + result : result;
        }
    }

    public static String getRecipeName(String name) {
        String result = recipe.get(name);
        if (result != null && !result.isEmpty()) {
            return result;
        } else {
            if (debug && getLanguage() != getDefaultLanguage() && !debugRecipe.contains(name)) {
                debugRecipe.add(name);
            }

            return name;
        }
    }

    public static String getRecipeGroupName(String name) {
        String result = recipeGroups.get(name);
        if (result != null && !result.isEmpty()) {
            return result;
        } else {
            if (debug && getLanguage() != getDefaultLanguage()) {
                debugRecipeGroups.add(name);
            }

            return name;
        }
    }

    public static Language getDefaultLanguage() {
        return Languages.instance.getDefaultLanguage();
    }

    public static void debugItemEvolvedRecipeNames() {
        if (debug && !debugItemEvolvedRecipeName.isEmpty()) {
            debugwrite("EvolvedRecipeName_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>(debugItemEvolvedRecipeName);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tEvolvedRecipeName_" + name + " = \"" + itemEvolvedRecipeName.get(name) + "\",\r\n");
            }

            debugItemEvolvedRecipeName.clear();
        }
    }

    public static void debugItemNames() {
        if (debug && !debugItem.isEmpty()) {
            debugwrite("ItemName_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>(debugItem);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tItemName_" + name + " = \"" + itemName.get(name) + "\",\r\n");
            }

            debugItem.clear();
        }
    }

    public static void debugMultiStageBuildNames() {
        if (debug && !debugMultiStageBuild.isEmpty()) {
            debugwrite("MultiStageBuild_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>(debugMultiStageBuild);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tMultiStageBuild_" + name + " = \"\",\r\n");
            }

            debugMultiStageBuild.clear();
        }
    }

    public static void debugRecipeNames() {
        if (debug && !debugRecipe.isEmpty()) {
            debugwrite("Recipes_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>(debugRecipe);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tRecipe_" + name.replace(" ", "_") + " = \"\",\r\n");
            }

            debugRecipe.clear();
        }
    }

    public static void debugRecipeGroupNames() {
        if (debug && !debugRecipeGroups.isEmpty()) {
            debugwrite("RecipeGroups_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>(debugRecipeGroups);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tRecipeGroup_" + name.replace(" ", "_") + " = \"\",\r\n");
            }

            debugRecipeGroups.clear();
        }
    }

    private static void debugwrite(String s) {
        if (debugFile != null) {
            try {
                debugFile.write(s);
                debugFile.flush();
            } catch (IOException var2) {
            }
        }
    }

    public static ArrayList<String> getAzertyMap() {
        if (azertyLanguages == null) {
            azertyLanguages = new ArrayList<>();
            azertyLanguages.add("FR");
        }

        return azertyLanguages;
    }

    public static String getRadioText(String s) {
        String result = dynamicRadio.get(s);
        return result == null ? s : result;
    }

    public static String getTextMediaEN(String desc) {
        if (ui == null) {
            loadFiles();
        }

        String result = null;
        if (desc.startsWith("RM_")) {
            result = recordedMedia_EN.get(desc);
        }

        String dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" : null;
        if (result == null) {
            if (!missing.contains(desc) && Core.debug) {
                if (Core.debug) {
                    DebugType.Translation.error("ERROR: Missing translation \"" + desc + "\"");
                }

                if (debug) {
                    debugwrite("ERROR: Missing translation \"" + desc + "\"\r\n");
                }

                missing.add(desc);
            }

            result = desc;
            dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" : null;
        }

        if (result.contains("<br>")) {
            return result.replaceAll("<br>", "\n");
        } else {
            return dbg == null ? result : dbg + result;
        }
    }

    public static String getAttributeText(String s) {
        return getAttributeText(s, false);
    }

    public static String getAttributeTextOrNull(String s) {
        return getAttributeText(s, true);
    }

    private static String getAttributeText(String s, boolean nullOnFail) {
        String result = attributes.get(s);
        if (result == null) {
            if (!missing.contains(s)) {
                DebugType.Translation.error("ERROR: Missing translation \"" + s + "\"");
                if (debug) {
                    debugwrite("ERROR: Missing translation \"" + s + "\"\r\n");
                }

                missing.add(s);
            }

            return nullOnFail ? null : s;
        } else {
            return result;
        }
    }

    public static String getFluidText(String s) {
        String result = fluids.get(s);
        return result == null ? s : result;
    }

    public static String getEntityText(String s) {
        String result = entity.get(s);
        return result == null ? s : result;
    }

    public static String getMapLabelText(String s) {
        String result = mapLabel.get(s);
        return result == null ? s : result;
    }

    public static Map<String, String> getUI() {
        return ui;
    }
}
