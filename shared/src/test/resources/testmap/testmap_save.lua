--[[                                                                           ]]--                                        ]]----[[                                                                           ]]--
--[[  Scenario                                                                 ]]--
--[[                                                                           ]]--
Scenario = {
    next_area_id = '1',
    --[[                                                                           ]]--
    --[[  Props                                                                    ]]--
    --[[                                                                           ]]--
    Props = {
    },
    --[[                                                                           ]]--
    --[[  Areas                                                                    ]]--
    --[[                                                                           ]]--
    Areas = {
        ['AREA_1'] = {
            ['rectangle'] = RECTANGLE( 1, 2, 3, 4 ),
        },
    },
    --[[                                                                           ]]--
    --[[  Markers                                                                  ]]--
    --[[                                                                           ]]--
    MasterChain = {
        ['_MASTERCHAIN_'] = {
            Markers = {
                ['Mass 00'] = {
                    ['size'] = FLOAT( 1.000000 ),
                    ['resource'] = BOOLEAN( true ),
                    ['amount'] = FLOAT( 100.000000 ),
                    ['color'] = STRING( 'ff808080' ),
                    ['editorIcon'] = STRING( '/textures/editor/marker_mass.bmp' ),
                    ['type'] = STRING( 'Mass' ),
                    ['prop'] = STRING( '/env/common/props/markers/M_Mass_prop.bp' ),
                    ['orientation'] = VECTOR3( 0, -0, 0 ),
                    ['position'] = VECTOR3( 119.5, 64, 19.5 ),
                },
                ['Hydrocarbon 00'] = {
                    ['size'] = FLOAT( 3.000000 ),
                    ['amount'] = FLOAT( 100.000000 ),
                    ['color'] = STRING( 'ff008000' ),
                    ['resource'] = BOOLEAN( true ),
                    ['type'] = STRING( 'Hydrocarbon' ),
                    ['prop'] = STRING( '/env/common/props/markers/M_Hydrocarbon_prop.bp' ),
                    ['orientation'] = VECTOR3( 0, -0, 0 ),
                    ['position'] = VECTOR3( 47.5, 64, 124.5 ),
                },
                ['ARMY_1'] = {
                    ['color'] = STRING( 'ff800080' ),
                    ['type'] = STRING( 'Blank Marker' ),
                    ['prop'] = STRING( '/env/common/props/markers/M_Blank_prop.bp' ),
                    ['orientation'] = VECTOR3( 0, -0, 0 ),
                    ['position'] = VECTOR3( 45.5, 64, 209.5 ),
                },
            },
        },
    },
    Chains = {
    },
    --[[                                                                           ]]--
    --[[  Orders                                                                   ]]--
    --[[                                                                           ]]--
    next_queue_id = '1',
    Orders = {
    },
    --[[                                                                           ]]--
    --[[  Platoons                                                                 ]]--
    --[[                                                                           ]]--
    next_platoon_id = '1',
    Platoons =
    {
    },
    --[[                                                                           ]]--
    --[[  Armies                                                                   ]]--
    --[[                                                                           ]]--
    next_army_id = '9',
    next_group_id = '1',
    next_unit_id = '1',
    Armies =
    {
        --[[                                                                           ]]--
        --[[  Army                                                                     ]]--
        --[[                                                                           ]]--
        ['ARMY_1'] =
        {
            personality = '',
            plans = '',
            color = 0,
            faction = 0,
            Economy = {
                mass = 0,
                energy = 0,
            },
            Alliances = {
            },
            ['Units'] = GROUP {
                orders = '',
                platoon = '',
                Units = {
                    ['INITIAL'] = GROUP {
                        orders = '',
                        platoon = '',
                        Units = {
                        },
                    },
                },
            },
            PlatoonBuilders = {
                next_platoon_builder_id = '0',
                Builders = {
                },
            },
        },
        --[[                                                                           ]]--
        --[[  Army                                                                     ]]--
        --[[                                                                           ]]--
        ['NEUTRAL_CIVILIAN'] =
        {
            personality = '',
            plans = '',
            color = 0,
            faction = 0,
            Economy = {
                mass = 0,
                energy = 0,
            },
            Alliances = {
            },
            ['Units'] = GROUP {
                orders = '',
                platoon = '',
                Units = {
                    ['INITIAL'] = GROUP {
                        orders = '',
                        platoon = '',
                        Units = {
                            ['UNIT_1'] = {
                                type = 'xsc8001',
                                orders = '',
                                platoon = '',
                                Position = { 128.500000, 64.000000, 127.500000 },
                                Orientation = { 0.000000, 0.000000, 0.000000 },
                            },
                        },
                    },
                },
            },
            PlatoonBuilders = {
                next_platoon_builder_id = '0',
                Builders = {
                },
            },
        },
    },
}
