# Biomes
A biome is a set of materials, lighting and water parameters that gives a certain look to the map.
Biomes are like map "themes" - the biome is picked by the generator in a predefined list right at the end of the generation, in a random manner, and affects only the visuals. The biome has no impact on the generation itself whatsoever.

## Current biomes
You can find a list of all the existing biomes [here](https://github.com/FAForever/Neroxis-Map-Generator/blob/master/current_biomes.md).

I strongly recommend you to take a look at this list before making biomes yourself. Biomes that are copycats of existing biomes will be rejected.

# How are biomes made?
A biome is usually made of three elements :
  - **A list of materials [mandatory]**
  - A lighting settings file
  - A water shader file

These files can be created in three ways :
  - From an existing SC map file, using the automated tool
  - From an existing SC map file, using Ozonex editor + a bit of manual tweaking
  - From scratch using Notepad. As they are text files, modifying them with any text editor is possible.

Keep in mind that the usual workflow to create a biome is:
 1) Open any generated map zith Ozonex editor
 2) Fiddle with materials, lighting, and water, until you get something you like
 3) Export the three files from this map

For the rest of this document, I will consider that you already have a .SCMAP file that has the lighting, water and material set that you desire your biome to have.

### Side note
Maps with custom textures or elements (e.g. not taken from the basegame, sc, scfa or faf) are not supported and the export won't work as intended.

## Using the automated tool
You can download at [this address](https://github.com/FAForever/Neroxis-Map-Generator/blob/develop/tools/NexTemplateGenerator_v4.zip) a tool that automatically exports the water shader, lightning, and textures from an existing SCMap. Although it might support custom maps from the vault, only the support for stock maps is guaranteed (SC+FA)

1. Extract the zip
2. Open the extracted folder in a new file explorer window
3. Open the folder of your map in another file explorer window
4. Drag-and-drop the `your_map_name.SCMAP` file onto the `NeroxisTemplateGenerator.exe` file in the explorer window.
5. Once the program terminates, you should have a `your_map_name` folder in the same location than the program. This folder is a biome, ready to use for generation.
6. Profit! See "Testing your biome" part to make sure your biome looks as intended.

## Using Ozonex editor + Notepad


# Testing your biome

