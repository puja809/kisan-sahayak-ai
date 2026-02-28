# **Technical and Strategic Roadmap for the Development of an Integrated Digital Agricultural Ecosystem in India**

The structural evolution of the Indian agricultural sector is increasingly defined by the convergence of high-resolution geospatial data, real-time market intelligence, and the deployment of sophisticated Digital Public Infrastructure (DPI). The conceptualization of an "extreme resourceful application" designed to empower the Indian farmer necessitates a multifaceted integration of disparate data streams, ranging from meteorological nowcasting to multilingual conversational artificial intelligence. This report delineates the architectural requirements, data sources, and policy frameworks essential for constructing such a platform, emphasizing the synergy between centralized registries like AgriStack and decentralized extension services like the Krishi Vigyan Kendra (KVK) network.

## **Meteorological Intelligence and Precision Forecasting Architecture**

The primary layer of decision support for any agricultural enterprise is the accurate prediction of atmospheric conditions. In the Indian context, the India Meteorological Department (IMD) serves as the authoritative source for weather data. For a digital application to provide actionable insights, it must transcend simple temperature displays and integrate hyper-local "nowcasting" and district-level warnings that inform daily operations such as sowing, pesticide application, and harvesting schedules.

The IMD has transitioned towards an open-data framework, offering a suite of Application Programming Interfaces (APIs) that provide critical variables for field-level decision-making. Developers seeking to integrate these services must navigate a structured onboarding process, often requiring the whitelisting of public IP addresses to ensure secure and consistent access to the data stream.1 The utility of these APIs is not limited to immediate weather conditions but extends to complex variables such as nebulosity (cloud coverage on a scale of 0-8), relative humidity at specific diurnal intervals (0830 and 1730 hours), and quantitative precipitation forecasts (QPF) for river basins, which are essential for assessing flood risks.1

### **Strategic IMD API Integration Framework**

The integration of meteorological data requires a tiered approach, prioritizing high-frequency updates for immediate hazards while maintaining a broader 7-day outlook for seasonal planning.

| API Service Category | Endpoint Utility and Data Variables | Strategic Decision Support |
| :---- | :---- | :---- |
| City Weather Forecast (7-day) | Max/Min Temp, Humidity, Sunrise/Sunset, Moonrise/Moonset times. | Strategic planning for harvest cycles and labor scheduling.1 |
| Current Weather API | Real-time cloud coverage, 24-hr cumulative rainfall, wind vectors. | Real-time adjustments to irrigation and aerial spray schedules.1 |
| District Wise Nowcast | 0-3 hour high-frequency warnings for thunderstorms, squalls, and localized precipitation. | Immediate risk mitigation for livestock and sensitive open-field crops.1 |
| District Rainfall Stats | Actual vs. Normal rainfall comparison and percentage departure analysis. | Assessing drought vulnerability and long-term water management strategies.1 |
| AWS/ARG Real-time | Observations from Automated Weather Stations and Rain Gauges. | Ground-truthing satellite-based weather models for hyper-local precision.1 |

The technical implementation involves handling JSON responses that contain multi-day time-series data. For instance, a standard response includes forecasted maximum and minimum temperatures for seven consecutive days, allowing the application to visualize warming or cooling trends that might impact crop phenology.1 Furthermore, the inclusion of astronomical data such as moonrise and moonset times is often overlooked but remains culturally and agronomically relevant for traditional farming practices in various Indian states.2

## **Agro-Ecological Suitability and Precision Crop Recommendations**

To provide meaningful crop recommendations, an application must analyze a complex interplay of soil characteristics, climatic regimes, and historical performance data. The Indian Council of Agricultural Research (ICAR) has historically delineated the country into 20 distinct agro-ecological regions and 60 sub-regions, based on physiography, climate, soil types, and the length of the growing period (LGP).6 This classification ensures that recommendations are grounded in the natural productive capacity of the land.

### **Global Agro-Ecological Zones (GAEZ) and Spatial Analytics**

For developers, the Global Agro-Ecological Zones (GAEZ) framework, a collaborative effort between the FAO and IIASA, provides a standardized model for assessing land suitability.7 The GAEZ v4 data portal offers spatial indicators at a 5 arc-minute resolution, covering soil suitability, terrain slopes, and agro-climatic potential yields for up to 280 crops.7 By integrating these datasets, the application can recommend crops based on what the land is climatically and edaphically capable of producing under various input scenarios—such as rain-fed vs. irrigated conditions.9

The GAEZ data model is organized into thematic areas that are essential for high-fidelity crop modeling:

1. **Land and Water Resources**: Including soil resources, terrain, and land cover.8  
2. **Agro-climatic Potential Yield**: Eco-physiological crop growth models that calculate potential biomass under specific thermal and moisture regimes.9  
3. **Yield and Production Gaps**: Analyzing the difference between actual yields and potential yields to identify opportunities for improvement.7

### **Crop Rotation and Sustainable Intensification Strategies**

Crop rotation is not merely a practice of tradition but a sophisticated strategy for soil health management and risk mitigation. Evidence indicates that following specific rotation patterns, such as growing legumes after cereals, can significantly improve soil structure and nutrient profiles.10 In regions like Odisha, where the production system is dominated by rice, ICAR recommends diversifying into green gram, black gram, and oilseeds to leverage residual soil moisture post-harvest.11

A robust recommendation engine within the app must prioritize:

* **Nutrient Cycling Optimization**: Alternating deep-rooted crops, such as sunflower, with shallow-rooted crops, like cabbage, to ensure soil moisture and nutrients are utilized at varying depths.10  
* **Biological Nitrogen Fixation**: Systematically incorporating pulses (greengram, blackgram, redgram) to replenish soil nitrogen naturally and reduce dependency on chemical fertilizers.11  
* **Climate Resilience via Intercropping**: Using intercropping or relay cropping (e.g., sowing lentil or gram into a maturing rice crop, known as "paira" or "utera" cropping) to optimize land use and stabilize household income.12

### **Soil Health Integration and Nutrient Management**

The Soil Health Card (SHC) scheme provides a digitized record of nutrient status for millions of farm plots across India. The SHC portal facilitates the generation of cards in 22 languages and 5 dialects, providing recommendations based on 12 parameters, including primary nutrients (N, P, K), secondary nutrients (S), and micronutrients (Zn, Fe, Cu, Mn, B).13 The SHC system uses a standardized workflow involving sample collection, registration, and testing in accredited laboratories to generate automatic fertilizer recommendations.13 For the developer, these datasets can be visualized through pictorial soil maps, helping farmers understand the specific nutrient deficiencies of their land parcels.13

## **Real-Time Commodity Market Dynamics and Mandi Price Analytics**

The Agmarknet portal, managed by the Directorate of Marketing and Inspection (DMI) under the Ministry of Agriculture and Farmers Welfare, serves as the "single source of truth" for wholesale prices across more than 3,000 regulated agricultural markets (mandis) in India.15 Accessing this data via the Agmarknet API is foundational for ensuring market transparency and enabling farmers to make informed selling decisions.

### **Agmarknet API Capabilities and Data Architecture**

The Agmarknet API processes approximately 2 million price records monthly, covering more than 200 agricultural commodities, including cereals, pulses, vegetables, and fruits.15 The data is recorded at the mandi level by regulated market staff who input daily arrivals and prices directly into a central, near real-time database.15

| Market Data Field | Technical Description and Context | Strategic Utility |
| :---- | :---- | :---- |
| Modal Price | The most frequent price at which a commodity is traded on a given day. | Acts as the primary indicator of current market value for the average producer.17 |
| Min/Max Price | The lower and upper bounds of price discovery for a specific variety and grade. | Helps farmers understand the price spread and potential premiums for higher quality.15 |
| Arrival Quantity | The volume of the commodity arriving at the market, measured in quintals. | Facilitates supply-demand analysis; high arrivals often signal impending price drops.15 |
| Commodity Variety | Specific classification of the crop (e.g., Hybrid vs. Desi varieties). | Critical for accurate comparisons, as different varieties command vastly different market prices.16 |
| Mandi Location | Geo-location of the regulated market. | Allows the app to provide a "Mandi Comparison" feature to find the best price within a certain radius.15 |

The integration of Agmarknet data enables sophisticated "Crop Price Alert" features, where farmers receive push notifications when prices for their specific crops peak in neighboring districts.15 Furthermore, historical price data available through the API allows for time-series analysis and price forecasting, which can guide farmers on the most strategic timing for post-harvest sales.15

## **Digital Public Infrastructure: AgriStack and the Unified Farmer Registry**

The most ambitious component of the proposed application is its integration into the emerging Digital Public Infrastructure (DPI) for agriculture. The Government of India is currently deploying **AgriStack**, a farmer-centric ecosystem designed to create a "single, verified source of truth" for agricultural data.19 AgriStack is built upon the InDEA 2.0 (India Digital Ecosystem Architecture 2.0) framework, emphasizing interoperability, open API standards, and privacy-by-design.19

### **The Core Registries of AgriStack**

AgriStack operates through three federated and authenticated registries that collectively answer the core questions of agricultural administration: "who is the farmer," "where is the land," and "what is growing on it".19

1. **Farmer Registry**: This registry assigns a unique, digitally verifiable **Farmer ID** to every cultivator. As of May 2025, over 6 crore Farmer IDs have been generated across 17 states.19 This ID serves as a functional identifier, based on Aadhaar, ensuring that government support and subsidies reach the intended beneficiaries.21  
2. **Geo-Referenced Village Map Registry**: This component links the Farmer ID to specific, geo-referenced land parcels, allowing for plot-level precision in service delivery.19  
3. **Crop Sown Registry**: Populated through **Digital Crop Surveys (DCS)**, this registry provides real-time data on seasonal crop patterns. During the 2024-25 Rabi season pilot, over 253 million farm plots were mapped across 17 states.19

For a developer, AgriStack provides the **Unified Farmer Service Interface (UFSI)**, an open API gateway that facilitates seamless data exchange between authorized public and private sector applications.19 This ecosystem enables "instant, paper-free credit," where financial institutions pull verified land and crop details to approve working-capital loans in under 30 minutes—a process that previously took several weeks.19

### **Implementation of the Digital Crop Survey (DCS)**

The DCS represents a paradigm shift in agricultural monitoring. Using a mobile-based application, local village youth act as crop surveyors, capturing geo-tagged and time-stamped images of crops approximately one month after sowing.22 The system leverages artificial intelligence (AI/ML) to auto-verify crop signatures (e.g., wheat, rice, mustard) from these images, reducing the workload for human supervisors and ensuring high data accuracy (reported at 93%–100% in pilot studies).22 This real-time data is critical for the "Crop Weather Watch," drought monitoring, and the settlement of insurance claims.20

## **National and State-Specific Welfare Frameworks**

The delivery of government schemes is a primary driver for app adoption among the farming community. The application must provide a localized interface that bridges central initiatives with state-specific programs.

### **Central Government Schemes and Insurance Architecture**

The app should serve as a gateway to major central sector schemes:

* **PM-Kisan Samman Nidhi**: Provides direct income support of ₹6,000 per year in three installments of ₹2,000 each.24  
* **Pradhan Mantri Fasal Bima Yojana (PMFBY)**: A comprehensive crop insurance scheme protecting farmers against non-preventable natural risks such as drought, floods, and pests.26  
* **PM-Kisan Maan Dhan Yojana**: A social security pension scheme providing ₹3,000 per month to small and marginal farmers after the age of 60\.26  
* **Kisan Credit Card (KCC)**: Facilitates institutional credit for crops and animal husbandry. Recent expansions include KCC for fisheries and dairy.24

### **State-Specific Digital Ecosystems and Portals**

Agriculture is a state subject in India, and many of the most impactful schemes are managed at the provincial level. The application must integrate with the unique digital portals of each state.

#### **Karnataka: The FRUITS and Bhoomi Ecosystem**

The **FRUITS** (Farmer Registration & Unified Beneficiary Information System) portal in Karnataka is a pioneering e-governance initiative that creates a "Golden Record" for every farmer.28

* **Integration**: It integrates with the **Bhoomi** land records database and the **Kutumba** family registry to ensure data authenticity.29  
* **Benefit Distribution**: FRUITS enables entitlement-based benefit distribution, where the system automatically identifies eligible farmers for schemes like Minimum Support Price (MSP) registration or milk subsidies without requiring a fresh application for every scheme.29  
* **Specific Schemes**: The **Krishi Bhagya** scheme provides financial assistance of up to ₹50,000 for micro-irrigation and ₹1 lakh for farm ponds to small and marginal farmers.31

#### **Maharashtra: MahaDBT and PMKSY**

In Maharashtra, the **MahaDBT** portal is the centralized platform for direct benefit transfers across agricultural, social welfare, and education departments.32

* **Mechanization Subsidies**: Farmers can apply for subsidies on tractors, harvesters, and power tillers. A "First Come, First Served" approach is being implemented for application selection starting in 2025-26.32  
* **Pradhan Mantri Krishi Sinchayee Yojana (PMKSY)**: Offers 55% subsidies to small/marginal farmers and 45% to other farmers for micro-irrigation components (drip and sprinkler systems).35

#### **Telangana: Rythu Bandhu and Rythu Bima**

Telangana's welfare model is characterized by high-impact, direct support programs:

* **Rythu Bandhu**: An investment support scheme providing ₹10,000 per acre per year (₹5,000 each for the Kharif and Rabi seasons) to support the purchase of inputs like seeds and fertilizers.37  
* **Rythu Bima**: A life insurance scheme providing ₹5,00,000 to the nominee in the event of a farmer's death. Uniquely, the entire premium is borne by the State Government, and the claim process is entirely online, facilitated through Agriculture Extension Officers (AEO).38

#### **Andhra Pradesh: YSR Rythu Bharosa**

The **YSR Rythu Bharosa** scheme provides financial assistance of ₹13,500 per year per farmer family.40

* **Inclusivity**: Unlike many other state schemes, it specifically includes landless tenant farmers and cultivators belonging to SC, ST, BC, and minority categories.41  
* **Ancillary Support**: Includes 9 hours of free daytime electricity for agriculture, free borewell facilities, and the establishment of cold storage and food processing units at the constituency level.40

#### **Haryana: Meri Fasal Mera Byora**

The **Meri Fasal Mera Byora** portal is mandatory for Haryana farmers to register their crop sowing details, which is then linked to the e-Kharid procurement network.43

* **Requirements**: Farmers must use their **Parivar Pehchan Patra** (Family ID) for registration.43  
* **Benefits**: Ensures procurement at MSP, access to crop insurance, and direct payment into bank accounts for various subsidies and compensation for crop damage due to natural calamities.43

#### **Uttar Pradesh and Punjab**

* **Uttar Pradesh**: The state is implementing the **UP Farmer Registry** (UPFR) under the AgriStack framework, issuing unique Farmer IDs to streamline the delivery of subsidies for marigold farming, vermicompost units, and solar-powered irrigation.46  
* **Punjab**: The **Punjab Agriculture Subsidy 2025** provides up to a 60% subsidy on modern farm machinery, including laser land levelers and rice transplanters, to promote mechanization and reduce water waste.48

## **Institutional Infrastructure: Extension Services and Local Support**

To fulfill the requirement of locating the nearest government body, the application must integrate a geo-directory of the decentralized agricultural support network.

### **Krishi Vigyan Kendra (KVK) Network**

KVKs are the primary frontline centers for agricultural technology application in India, managed by ICAR and various host organizations (SAUs, NGOs, ICAR Institutes).49 There are currently over 700 KVKs operating at the district level across the country.49

KVKs fulfill a critical mandate:

* **On-farm Testing**: Assessing the location-specificity of agricultural technologies under various farming systems.49  
* **Frontline Demonstrations**: Showcasing the production potential of newly released varieties on farmers' fields.49  
* **Capacity Development**: Conducting training programs for farmers and rural youth to update their technical knowledge and skills.49

The app should leverage existing telephone directories and GPS-based boundary maps of KVKs to provide farmers with direct contact information for Senior Scientists and Heads in specialized disciplines such as Horticulture, Soil Science, and Plant Protection.52 These KVKs are overseen by 11 **Agricultural Technology Application Research Institutes (ATARI)**, which manage the regional coordination of extension activities.49

## **Multilingual Conversational AI: The Bhashini Framework**

A critical barrier to app adoption in rural India is the linguistic diversity of the user base. With 22 constitutionally recognized languages and numerous regional dialects, a text-heavy English or Hindi interface is insufficient. The **Digital India Bhashini** mission provides the technological stack to overcome this through a "multilingual voice agent."

### **Bhashini’s Language AI Technology Stack**

Bhashini offers a comprehensive ecosystem of APIs and datasets specifically designed for the Indian linguistic landscape.56

| AI Component | Functional Capability and Language Support | Technical Delivery |
| :---- | :---- | :---- |
| **ASR (Speech-to-Text)** | Transcribes 22+ Indian languages and English. Includes specific models for dialects like Bhojpuri and Maithili.58 | Real-time streaming via WebSocket APIs with built-in Voice Activity Detection (VAD).59 |
| **NMT (Translation)** | Context-aware translation across all 22 languages. Optimized for low-latency, cross-lingual communication.56 | RESTful APIs for sentence-level and document-level translation.56 |
| **TTS (Text-to-Speech)** | Generates natural-sounding, expressive human voices for 22 languages and Indian-accented English.56 | Streaming audio synthesis optimized for conversational turn-taking.58 |
| **OCR & Vision** | Extracts text from printed pages or signboards across all Indian scripts.56 | Enables the app to "read" physical documents (like SHCs or Land Records) aloud to the farmer.56 |

### **Implementing a Full-Duplex Voice Agent**

The technical architecture of the voice agent follows a complex pipeline of tasks: \`\`.60

1. **Audio Ingestion**: The farmer's speech is captured via a microphone and streamed to Bhashini’s ASR service. The system uses VAD to identify when the user has finished speaking.59  
2. **Linguistic Processing**: The transcribed native speech is translated into a target language (e.g., English) for processing by an LLM (Large Language Model) or a domain-specific dialog engine.60  
3. **Conversational Intelligence**: The dialog engine maintains context and invokes external APIs (like Agmarknet for prices or IMD for weather) to find the requested information.60  
4. **Audio Synthesis**: The response is translated back into the farmer's native language and converted to speech using Bhashini’s TTS models, which are trained on professional voice artists to ensure human-like warmth and clarity.56

Bhashini also facilitates "sectoral specialization," allowing the AI to be grounded in the specific terminology of agriculture.57 Researchers have demonstrated that fine-tuned encoder models, specifically trained on agricultural datasets like "AgriNLP," can significantly outperform generic models like ChatGPT in recognizing agricultural entities and relations.63

## **Technical Synthesis and Strategic Implementation**

The successful deployment of an "extreme resourceful app" for Indian farmers requires a modular architecture that can harmonize these diverse and often siloed data streams.

### **Core Architectural Layers**

* **Geo-Context Layer**: Utilizes the mobile device's GPS to determine the user's location, which serves as the primary key for querying localized weather (IMD), identifying the nearest mandi (Agmarknet), and locating the relevant extension center (KVK).1  
* **Registry Integration Layer**: Leverages the AgriStack UFSI to authenticate the farmer via their unique Farmer ID. This allows the app to pre-populate land record details from state databases (like Bhoomi or Dharani) and provide personalized scheme recommendations.19  
* **Predictive Analytics Engine**: Combines ICAR’s agro-ecological zoning with GAEZ v4 suitability maps and real-time IMD data to provide dynamic crop rotation and sowing advisories.6  
* **Conversational Interface**: A WebSocket-driven voice agent that utilizes the Bhashini stack to provide a frictionless, "zero-typing" experience for the farmer, making complex information accessible regardless of literacy levels.56

### **Future Outlook and Digital Sovereignty**

The move toward a unified agricultural digital ecosystem is not merely a technological upgrade but a fundamental shift in how agricultural services are delivered. By transitioning from traditional extension models to real-time, data-driven advisories, the Indian farmer is empowered with digital sovereignty—the ability to access verified, authoritative information and government benefits directly through a single, multilingual platform. The convergence of AgriStack, Bhashini, and the specialized research of ICAR provides the technical foundation for this transformation, ensuring that the next generation of Indian agriculture is resilient, transparent, and digitally inclusive.

#### **Works cited**

1. List of API's of India Meteorological Department, accessed February 10, 2026, [https://mausam.imd.gov.in/imd\_latest/contents/api.pdf](https://mausam.imd.gov.in/imd_latest/contents/api.pdf)  
2. abin-m/Weather-api-imd \- GitHub, accessed February 10, 2026, [https://github.com/abin-m/Weather-api-imd](https://github.com/abin-m/Weather-api-imd)  
3. IMD Weather API Documentation List | PDF \- Scribd, accessed February 10, 2026, [https://www.scribd.com/document/685569962/imd-weather-api](https://www.scribd.com/document/685569962/imd-weather-api)  
4. India Meteorological Department (IMD) \- Open Government Data (OGD) Platform India, accessed February 10, 2026, [https://www.data.gov.in/ministrydepartment/India%20Meteorological%20Department%20(IMD)](https://www.data.gov.in/ministrydepartment/India%20Meteorological%20Department%20\(IMD\))  
5. Weather API \- Indian API Marketplace, accessed February 10, 2026, [https://indianapi.in/weather-api](https://indianapi.in/weather-api)  
6. Landmark Technologies | ICAR, accessed February 10, 2026, [https://www.icar.org.in/en/landmark-technologies](https://www.icar.org.in/en/landmark-technologies)  
7. GAEZ v4 Data Portal, accessed February 10, 2026, [https://gaez.fao.org/](https://gaez.fao.org/)  
8. GAEZ \- Global Agro-Ecological Zones | Information portals \- Climate-ADAPT, accessed February 10, 2026, [https://climate-adapt.eea.europa.eu/en/metadata/portals/gaez-global-agro-ecological-zones](https://climate-adapt.eea.europa.eu/en/metadata/portals/gaez-global-agro-ecological-zones)  
9. The Global Agro-Ecological Zoning Platform version 4 \- ArcGIS StoryMaps, accessed February 10, 2026, [https://storymaps.arcgis.com/stories/313ec768e0964866b0e3ec5d720ab3b1](https://storymaps.arcgis.com/stories/313ec768e0964866b0e3ec5d720ab3b1)  
10. Crop Rotation & Intercropping in India \- CEEW, accessed February 10, 2026, [https://www.ceew.in/publications/sustainable-agriculture-india/crop-rotation-intercropping](https://www.ceew.in/publications/sustainable-agriculture-india/crop-rotation-intercropping)  
11. crop planning and crop calendar for different agro-climatic zones of odisha \- Central Rice Research Institute, accessed February 10, 2026, [https://icar-nrri.in/wp-content/uploads/2021/11/RB-30.pdf](https://icar-nrri.in/wp-content/uploads/2021/11/RB-30.pdf)  
12. Rice-based cropping systems \- Indian Council of Agricultural Research Krishi Bhavan, accessed February 10, 2026, [https://icar.org.in/sites/default/files/inline-files/Rice-based-cropping-systems.pdf](https://icar.org.in/sites/default/files/inline-files/Rice-based-cropping-systems.pdf)  
13. Soil Health Card Portal | National Informatics Centre | India, accessed February 10, 2026, [https://www.nic.gov.in/project/soil-health-card-portal/](https://www.nic.gov.in/project/soil-health-card-portal/)  
14. Soil Health Card, accessed February 10, 2026, [https://soilhealth.dac.gov.in/soilhealthcard](https://soilhealth.dac.gov.in/soilhealthcard)  
15. Agmarknet API Access: Crop Prices & Market Data India \- Farmonaut, accessed February 10, 2026, [https://farmonaut.com/api-development/agmarknet-api-access-crop-prices-market-data-india](https://farmonaut.com/api-development/agmarknet-api-access-crop-prices-market-data-india)  
16. Current Daily Price of Various Commodities from Various Markets (Mandi), accessed February 10, 2026, [https://www.data.gov.in/resource/current-daily-price-various-commodities-various-markets-mandi](https://www.data.gov.in/resource/current-daily-price-various-commodities-various-markets-mandi)  
17. Variety wise Daily Market Prices of Commodity \- AIKosh, accessed February 10, 2026, [https://aikosh.indiaai.gov.in/home/datasets/details/variety\_wise\_daily\_market\_prices\_of\_commodity.html](https://aikosh.indiaai.gov.in/home/datasets/details/variety_wise_daily_market_prices_of_commodity.html)  
18. Indian Agricultural Mandi Prices (2023–2025) \- Kaggle, accessed February 10, 2026, [https://www.kaggle.com/datasets/arjunyadav99/indian-agricultural-mandi-prices-20232025](https://www.kaggle.com/datasets/arjunyadav99/indian-agricultural-mandi-prices-20232025)  
19. Digital Public Infrastructure for Agriculture – AgriStack \- ISSCA, accessed February 10, 2026, [https://issca.icrisat.org/scalable-solutions/digital-public-infrastructure-for-agriculture-agristack](https://issca.icrisat.org/scalable-solutions/digital-public-infrastructure-for-agriculture-agristack)  
20. Crop Survey \- Agri Stack, accessed February 10, 2026, [https://brdcs.agristack.gov.in/crop-survey-br/](https://brdcs.agristack.gov.in/crop-survey-br/)  
21. AgriStack: A DPI approach to transform Indian agriculture \- MicroSave Consulting (MSC), accessed February 10, 2026, [https://www.microsave.net/2025/11/12/agristack-a-dpi-approach-to-transform-indian-agriculture/](https://www.microsave.net/2025/11/12/agristack-a-dpi-approach-to-transform-indian-agriculture/)  
22. Agri Stack \- Department of Agriculture & Farmers Welfare, accessed February 10, 2026, [https://agriwelfare.gov.in/sites/MinistryLettersCompendium/Presentations/CKO%20Deck%20updated\_v8%20(1).pdf](https://agriwelfare.gov.in/sites/MinistryLettersCompendium/Presentations/CKO%20Deck%20updated_v8%20\(1\).pdf)  
23. Krishi DSS, accessed February 10, 2026, [https://krishi-dss.gov.in/](https://krishi-dss.gov.in/)  
24. Department of Agriculture and Farmers Welfare : Schemes / Programmes / Missions / Applications \- Integrated Government Online Directory, accessed February 10, 2026, [https://igod.gov.in/organization/QNe83XQBYNG-XPnvjOsx/SPMA/list](https://igod.gov.in/organization/QNe83XQBYNG-XPnvjOsx/SPMA/list)  
25. List of schemes of the government of India \- Wikipedia, accessed February 10, 2026, [https://en.wikipedia.org/wiki/List\_of\_schemes\_of\_the\_government\_of\_India](https://en.wikipedia.org/wiki/List_of_schemes_of_the_government_of_India)  
26. Schemes for Farmers | Vikaspedia \- Schemes, accessed February 10, 2026, [https://schemes.vikaspedia.in/viewcontent/schemesall/schemes-for-farmers?lgn=en](https://schemes.vikaspedia.in/viewcontent/schemesall/schemes-for-farmers?lgn=en)  
27. Punjab Government Schemes | Farmer Assistance \- Sant Baba Bhag Singh University, accessed February 10, 2026, [https://sbbsuniversity.ac.in/farmers%20app/schemes.html](https://sbbsuniversity.ac.in/farmers%20app/schemes.html)  
28. Farmer Registration and Unified beneficiary InformaTion System FRUITS, accessed February 10, 2026, [https://cdnbbsr.s3waas.gov.in/s3dcf6070a4ab7f3afbfd2809173e0824b/uploads/2025/09/202509081045787758.pdf](https://cdnbbsr.s3waas.gov.in/s3dcf6070a4ab7f3afbfd2809173e0824b/uploads/2025/09/202509081045787758.pdf)  
29. Farmer Registration & Unified beneficiary InformaTion System(FRUITS) KARNATAKA \- NeGD, accessed February 10, 2026, [https://negd.gov.in/isl/Directory/statedata/408](https://negd.gov.in/isl/Directory/statedata/408)  
30. Farmer Registration and Uniﬁed Beneﬁciary Information System | National Informatics Centre | India, accessed February 10, 2026, [https://www.nic.gov.in/project/fruits/](https://www.nic.gov.in/project/fruits/)  
31. Application Process Online Visit the Official Website: Go to the Karnataka Government's agriculture department \- Wegopals, accessed February 10, 2026, [https://www.wegopals.com/wp-content/uploads/2025/01/KRISHI-BHAGYA-SCHEME.pdf](https://www.wegopals.com/wp-content/uploads/2025/01/KRISHI-BHAGYA-SCHEME.pdf)  
32. Farmer Schemes \- Maha DBT \- SchemeData, accessed February 10, 2026, [https://mahadbt.maharashtra.gov.in/Farmer/SchemeData/SchemeData?str=E9DDFA703C38E51AD61F6F45B7D0D582](https://mahadbt.maharashtra.gov.in/Farmer/SchemeData/SchemeData?str=E9DDFA703C38E51AD61F6F45B7D0D582)  
33. MahaDBT – Login, Farmer & Student Scholarship Portal, accessed February 10, 2026, [https://maha-dbt.com/](https://maha-dbt.com/)  
34. State Agriculture Mechanization Scheme \- Maha DBT, accessed February 10, 2026, [https://mahadbt.maharashtra.gov.in/Farmer/SchemeData/SchemeData?str=E9DDFA703C38E51A147B39AD4D6A9082](https://mahadbt.maharashtra.gov.in/Farmer/SchemeData/SchemeData?str=E9DDFA703C38E51A147B39AD4D6A9082)  
35. Pradhan Mantri Krishi Sinchayee Yojana \- Per Drop More Crop (Micro-irrigation Component) \- Maha DBT, accessed February 10, 2026, [https://mahadbt.maharashtra.gov.in/Farmer/SchemeData/SchemeData?str=E9DDFA703C38E51AC7B56240D6D84F28](https://mahadbt.maharashtra.gov.in/Farmer/SchemeData/SchemeData?str=E9DDFA703C38E51AC7B56240D6D84F28)  
36. Food grains, Oil seeds, Sugarcane and Cotton \- Maha DBT, accessed February 10, 2026, [https://mahadbt.maharashtra.gov.in/Farmer/SchemeData/SchemeData?str=E9DDFA703C38E51A64CDF3CE7D352F8F](https://mahadbt.maharashtra.gov.in/Farmer/SchemeData/SchemeData?str=E9DDFA703C38E51A64CDF3CE7D352F8F)  
37. Rythu Bandhu Scheme in Telangana \- All Details \- IndiaFilings, accessed February 10, 2026, [https://www.indiafilings.com/learn/rythu-bandhu](https://www.indiafilings.com/learn/rythu-bandhu)  
38. Rythu Bima Scheme \- myScheme, accessed February 10, 2026, [https://www.myscheme.gov.in/schemes/rythu-bima](https://www.myscheme.gov.in/schemes/rythu-bima)  
39. Telangana Rythu Bima Farmer Insurance Scheme, accessed February 10, 2026, [https://www.govtschemes.in/rythu-bima-farmer-insurance-scheme](https://www.govtschemes.in/rythu-bima-farmer-insurance-scheme)  
40. Check Complete Info about Rythu Bharosa Scheme \- BankBazaar, accessed February 10, 2026, [https://www.bankbazaar.com/saving-schemes/rythu-bharosa-scheme.html](https://www.bankbazaar.com/saving-schemes/rythu-bharosa-scheme.html)  
41. YSR Rythu Bharosa Scheme | Govt Schemes India, accessed February 10, 2026, [https://www.govtschemes.in/ysr-rythu-bharosa-scheme](https://www.govtschemes.in/ysr-rythu-bharosa-scheme)  
42. YSR Rythu Bharosa: Objectives, Eligibility & Application Process \- Digit Insurance, accessed February 10, 2026, [https://www.godigit.com/government-schemes/what-is-ysr-rythu-bharosa-eligibility](https://www.godigit.com/government-schemes/what-is-ysr-rythu-bharosa-eligibility)  
43. Meri Fasal Mera Byora Kharif MSP Registration in 12 Easy Steps \- Forum, accessed February 10, 2026, [https://globalriskcommunity.com/forum/topics/meri-fasal-mera-byora-kharif-msp-registration-in-12-easy-steps](https://globalriskcommunity.com/forum/topics/meri-fasal-mera-byora-kharif-msp-registration-in-12-easy-steps)  
44. Haryana Government Has Reopened The Registration On Meri Fasal \- Mera Byora Portal So That The Farmers Can Register The Details Of Their Rabi \- Global Agriculture, accessed February 10, 2026, [https://www.global-agriculture.com/state-news/haryana-government-has-reopened-the-registration-on-meri-fasal-mera-byora-portal-so-that-the-farmers-can-register-the-details-of-their-rabi/](https://www.global-agriculture.com/state-news/haryana-government-has-reopened-the-registration-on-meri-fasal-mera-byora-portal-so-that-the-farmers-can-register-the-details-of-their-rabi/)  
45. Meri Fasal Mera Byora \- IndiaFilings, accessed February 10, 2026, [https://www.indiafilings.com/learn/meri-fasal-mera-byora/](https://www.indiafilings.com/learn/meri-fasal-mera-byora/)  
46. UPFR Agristack: Farmer Registry UP, Login, Registration, Status, Last Date @ upfr.agristack.gov.in \- UPFR Agristack, accessed February 10, 2026, [https://upfragristack.com/](https://upfragristack.com/)  
47. Uttar Pradesh Farmer Schemes: Boost Your Farming Success, accessed February 10, 2026, [https://subsistencefarming.in/en/government-schemes-for-farmers-in-uttar-pradesh/](https://subsistencefarming.in/en/government-schemes-for-farmers-in-uttar-pradesh/)  
48. Punjab Agriculture Subsidy 2025 – 60% Subsidy on Modern Farming Machinery, accessed February 10, 2026, [https://pakistangovernmentschemes.com/punjab-agriculture-subsidy-2025/](https://pakistangovernmentschemes.com/punjab-agriculture-subsidy-2025/)  
49. Krishi Vigyan Kendra \- TELEPHONE \- DIRECTORY, accessed February 10, 2026, [https://icar.org.in/sites/default/files/inline-files/KVK-Telephone-Directory.pdf](https://icar.org.in/sites/default/files/inline-files/KVK-Telephone-Directory.pdf)  
50. Krishi Vigyan Kendra (KVK) \- Open Government Data (OGD) Platform India, accessed February 10, 2026, [https://karnataka.data.gov.in/catalog/krishi-vigyan-kendra-kvk](https://karnataka.data.gov.in/catalog/krishi-vigyan-kendra-kvk)  
51. State/UT-wise Number of Krishi Vigyan Kendras (KVKs) in the Country as on 31-01-2025, accessed February 10, 2026, [https://kerala.data.gov.in/resource/stateut-wise-number-krishi-vigyan-kendras-kvks-country-31-01-2025](https://kerala.data.gov.in/resource/stateut-wise-number-krishi-vigyan-kendras-kvks-country-31-01-2025)  
52. Contact \- KVK Kendrapara, accessed February 10, 2026, [https://kendraparakvk.org/contact.html](https://kendraparakvk.org/contact.html)  
53. Contact Us | ICAR-KRISHI VIGYAN KENDRA \- KVK Vijayapura, accessed February 10, 2026, [https://kvkvijayapura.org/new/contact-us-2/](https://kvkvijayapura.org/new/contact-us-2/)  
54. GPS based boundary map of Krishi Vigyan Kendra, Amritsar, India with different blocks, accessed February 10, 2026, [https://www.researchgate.net/figure/GPS-based-boundary-map-of-Krishi-Vigyan-Kendra-Amritsar-India-with-different-blocks\_fig1\_390162445](https://www.researchgate.net/figure/GPS-based-boundary-map-of-Krishi-Vigyan-Kendra-Amritsar-India-with-different-blocks_fig1_390162445)  
55. Krishi Vigyan Kendra Telephone Directory \- ATARI, Bengaluru, accessed February 10, 2026, [https://ataribengaluru.org/docs/oth/Tele\_2024.pdf](https://ataribengaluru.org/docs/oth/Tele_2024.pdf)  
56. Bhashini AI Solutions, accessed February 10, 2026, [https://www.bhashini.ai/](https://www.bhashini.ai/)  
57. Field Guide \- 1st Edition \- Yotta Shakti Cloud, accessed February 10, 2026, [https://bhashinimigrationns.sosnm1.shakticloud.ai:9024/bhashinistaticassets/bhashini-assets/website/Field%20Guide%20-%201st%20Edition%20%284%29%20%281%29.pdf](https://bhashinimigrationns.sosnm1.shakticloud.ai:9024/bhashinistaticassets/bhashini-assets/website/Field%20Guide%20-%201st%20Edition%20%284%29%20%281%29.pdf)  
58. Available Models for usage \- Bhashini APIs \- GitBook, accessed February 10, 2026, [https://dibd-bhashini.gitbook.io/bhashini-apis/available-models-for-usage](https://dibd-bhashini.gitbook.io/bhashini-apis/available-models-for-usage)  
59. WebSocket ASR API \- Bhashini APIs \- GitBook, accessed February 10, 2026, [https://dibd-bhashini.gitbook.io/bhashini-apis/websocket-asr-api](https://dibd-bhashini.gitbook.io/bhashini-apis/websocket-asr-api)  
60. Bhashini.ai API Docs, accessed February 10, 2026, [https://www.bhashini.ai/docs](https://www.bhashini.ai/docs)  
61. Bhashini APIs: Overall Understanding of the API Calls, accessed February 10, 2026, [https://dibd-bhashini.gitbook.io/bhashini-apis](https://dibd-bhashini.gitbook.io/bhashini-apis)  
62. Speech Synthesis \- AI4Bharat, accessed February 10, 2026, [https://ai4bharat.iitm.ac.in/areas/tts](https://ai4bharat.iitm.ac.in/areas/tts)  
63. Fine-tuned encoder models with data augmentation beat ChatGPT in agricultural named entity recognition and relation extraction | Request PDF \- ResearchGate, accessed February 10, 2026, [https://www.researchgate.net/publication/389724974\_Fine-tuned\_encoder\_models\_with\_data\_augmentation\_beat\_ChatGPT\_in\_agricultural\_named\_entity\_recognition\_and\_relation\_extraction](https://www.researchgate.net/publication/389724974_Fine-tuned_encoder_models_with_data_augmentation_beat_ChatGPT_in_agricultural_named_entity_recognition_and_relation_extraction)