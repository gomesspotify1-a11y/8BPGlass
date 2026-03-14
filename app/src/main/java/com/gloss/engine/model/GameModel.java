package com.glass.engine.model;

public class GameModel {
    private boolean isNew;
    private boolean requiresRoot;
    private boolean isPopular;
    private boolean isBestRated;
    private String gamenameincard;
    private String gameversionincard;
    private int gamecardimage;
    private int gamecardoverlayimage;
    private int fragmentimage;
    private int gameIcon; // новое поле для game_icon
    private String fragmentgamename;
    private String fragmentpublisher;
    private String fragmentdescription;
    private String basicDescription; // описание для Basic
    private String advancedDescription; // описание для Advanced
    private String ultimateDescription; // описание для Ultimate
    private String gamepackagename;

    public GameModel(
            String gamenameincard,
            String gameversionincard,
            int gamecardimage,
            int gamecardoverlayimage,
            int fragmentimage,
            int gameIcon,
            String fragmentgamename,
            String fragmentpublisher,
            String fragmentdescription,
            String basicDescription,
            String advancedDescription,
            String ultimateDescription,
            String gamepackagename,
            boolean isNew,
            boolean requiresRoot,
            boolean isPopular,
            boolean isBestRated
    ) {
        this.gamenameincard = gamenameincard;
        this.gameversionincard = gameversionincard;
        this.gamecardimage = gamecardimage;
        this.gamecardoverlayimage = gamecardoverlayimage;
        this.fragmentimage = fragmentimage;
        this.gameIcon = gameIcon;
        this.fragmentgamename = fragmentgamename;
        this.fragmentpublisher = fragmentpublisher;
        this.fragmentdescription = fragmentdescription;
        this.basicDescription = basicDescription;
        this.advancedDescription = advancedDescription;
        this.ultimateDescription = ultimateDescription;
        this.gamepackagename = gamepackagename;
        this.isNew = isNew;
        this.requiresRoot = requiresRoot;
        this.isPopular = isPopular;
        this.isBestRated = isBestRated;
    }

    // Note: AddGame static factory method is not updated with new fields.

    public String getGamenameincard() {
        return gamenameincard;
    }

    public String getGameversionincard() {
        return gameversionincard;
    }

    public int getGamecardimage() {
        return gamecardimage;
    }

    public int getGamecardoverlayimage() {
        return gamecardoverlayimage;
    }

    public int getFragmentimage() {
        return fragmentimage;
    }

    public int getGameIcon() {
        return gameIcon;
    }

    public String getFragmentgamename() {
        return fragmentgamename;
    }

    public String getFragmentpublisher() {
        return fragmentpublisher;
    }

    public String getFragmentdescription() {
        return fragmentdescription;
    }

    public String getBasicDescription() {
        return basicDescription;
    }

    public String getAdvancedDescription() {
        return advancedDescription;
    }

    public String getUltimateDescription() {
        return ultimateDescription;
    }

    public String getGamepackagename() {
        return gamepackagename;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean requiresRoot() {
        return requiresRoot;
    }

    public boolean isPopular() {
        return isPopular;
    }

    public boolean isBestRated() {
        return isBestRated;
    }

    public static GameModel AddGame(
            String[] cardText, int[] images, int fragmentImage, int gameIcon, String[] fragmentText,
            boolean isNew, boolean requiresRoot, boolean isPopular, boolean isBestRated) {
        return new GameModel(
                cardText[0], // gamenameincard
                cardText[1], // gameversionincard
                images[0],   // gamecardimage
                images[1],   // gamecardoverlayimage
                fragmentImage,
                gameIcon,    // gameIcon
                fragmentText[0], // fragmentgamename
                fragmentText[1], // fragmentpublisher
                fragmentText[2], // fragmentdescription
                fragmentText[3], // basicDescription
                fragmentText[4], // advancedDescription
                fragmentText[5], // ultimateDescription
                fragmentText[6], // gamepackagename
                isNew,
                requiresRoot,
                isPopular,
                isBestRated
        );
    }
}