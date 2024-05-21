/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim;


import java.net.URL;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.algorithms.NetworkSegmentDoubleLinks;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import static org.matsim.contrib.drt.run.DrtControlerCreator.createScenarioWithDrtRouteFactory;

public class RunMatsim {

    public static void main(String[] args) {

        Config config;
        if (args == null || args.length == 0 || args[0] == null) {
            config = ConfigUtils.loadConfig("./matsim-config.xml");
        } else {
            config = ConfigUtils.loadConfig(args);
        }

        //add RoadPricing ConfigGroup
        RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class);
        rpConfig.setTollLinksFile("cordonToll1.xml");

        config.controller().setOverwriteFileSetting((OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists));

        //load config into scenario
        Scenario scenario = createScenarioWithDrtRouteFactory(config);
        ScenarioUtils.loadScenario(scenario);

        //split parallel links between same nodes
        NetworkSegmentDoubleLinks algorithm = new NetworkSegmentDoubleLinks();
        algorithm.run(scenario.getNetwork());

        // define the toll factor as an anonymous class.  If more flexibility is needed, convert to "full" class.
        TollFactor tollFactor = (personId, vehicleId, linkId, time) -> {
            //          if (scenario.getVehicles().getVehicles().get(vehicleId).getType().getNetworkMode().equals("car")) {
            if (scenario.getVehicles().getVehicles().get(vehicleId) == null) {
                if (vehicleId.toString().contains("taxi")) {
                    return 1;
                } else {
                    return 0;
                }
            } else if (scenario.getVehicles().getVehicles().get(vehicleId).getType().getNetworkMode().equals("car")) {
                return 1;
            } else {
                return 0;
            }
        };

        // instantiate the road pricing scheme, with the toll factor inserted:
        URL roadpricingUrl;
        roadpricingUrl = IOUtils.extendUrl(config.getContext(), rpConfig.getTollLinksFile());
        RoadPricingSchemeUsingTollFactor rpricing = RoadPricingSchemeUsingTollFactor.createAndRegisterRoadPricingSchemeUsingTollFactor(roadpricingUrl, tollFactor, scenario);

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
        DvrpConfigGroup dvrpConfigGroup = ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
        //MultiModeTaxiConfigGroup multiModeTaxiConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeTaxiConfigGroup.class);

        config.checkConsistency();
        MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
        DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new DvrpModule());
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

        //create Controller and run
        controler.addOverridingModule(new RoadPricingModule( rpricing ));


        controler.run();

    }
}
