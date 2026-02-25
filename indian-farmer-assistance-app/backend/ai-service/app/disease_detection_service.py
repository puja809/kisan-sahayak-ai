"""Disease detection service for crop disease identification."""
import base64
import io
import time
import uuid
from datetime import datetime
from typing import List, Optional, Tuple

from PIL import Image

from app.config import settings
from app.logging_config import logger
from app.models import (
    BoundingBox,
    DiseaseDetectionRequest,
    DiseaseDetectionResponse,
    DiseaseDetectionResult,
    PreventiveMeasure,
    TreatmentOption,
    TreatmentRecommendation,
)


class DiseaseDetectionService:
    """Service for detecting crop diseases from images."""

    # Confidence threshold for disease detection
    CONFIDENCE_THRESHOLD = 70.0
    
    # Model version
    MODEL_VERSION = "1.0.0"
    
    # Supported crops for Indian agriculture
    SUPPORTED_CROPS = [
        "paddy", "rice", "wheat", "maize", "cotton", "sugarcane",
        "tomato", "potato", "onion", "chili", "brinjal", "cabbage",
        "cauliflower", "okra", "beans", "peas", "groundnut", "soybean",
        "mustard", "sunflower", "ginger", "turmeric", "banana", "mango",
        "grapes", "citrus", "pomegranate", "apple", "tea", "coffee"
    ]

    def __init__(self):
        """Initialize the disease detection service."""
        self._model = None
        self._model_loaded = False
        self._treatment_db = self._initialize_treatment_database()

    def _initialize_treatment_database(self) -> dict:
        """Initialize the treatment recommendation database."""
        return {
            "blast": {
                "disease_name": "Rice Blast",
                "disease_name_local": "धान का ब्लास्ट",
                "treatment_options": [
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Tricyclazole 75% WP",
                        description="Systemic fungicide for blast control",
                        dosage="0.6 g/L water",
                        application_timing="At disease onset and 15 days later",
                        safety_precautions=["Wear protective clothing", "Avoid inhalation", "Do not apply during flowering"],
                        estimated_cost=500
                    ),
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Azoxystrobin 23% SC",
                        description="Strobilurin class fungicide",
                        dosage="1.0 mL/L water",
                        application_timing="At disease onset",
                        safety_precautions=["Wear gloves and mask", "Keep away from water bodies"],
                        estimated_cost=650
                    ),
                    TreatmentOption(
                        type="ORGANIC",
                        name="Neem Oil 5%",
                        description="Organic fungicide from neem",
                        dosage="5 mL/L water",
                        application_timing="Weekly spray at disease onset",
                        safety_precautions=["Generally safe", "Avoid hot midday application"],
                        estimated_cost=300
                    )
                ],
                "preventive_measures": [
                    PreventiveMeasure(measure="Use resistant varieties", timing="At sowing", notes="IR 64, Swarna, MTU 1010"),
                    PreventiveMeasure(measure="Balanced fertilization", timing="Before sowing", notes="Avoid excess nitrogen"),
                    PreventiveMeasure(measure="Field sanitation", timing="Regular", notes="Remove infected plant debris"),
                    PreventiveMeasure(measure="Proper water management", timing="Throughout", notes="Avoid continuous flooding")
                ],
                "kvk_expert_contact": "Contact your local KVK for expert consultation"
            },
            "bacterial_blight": {
                "disease_name": "Bacterial Leaf Blight",
                "disease_name_local": "जीवाणु पत्ती झुलसा",
                "treatment_options": [
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Streptocycline",
                        description="Antibiotic for bacterial disease control",
                        dosage="0.3 g/L water + 10g Copper oxychloride",
                        application_timing="At disease onset, repeat after 10 days",
                        safety_precautions=["Use protective equipment", "Follow label instructions"],
                        estimated_cost=400
                    ),
                    TreatmentOption(
                        type="ORGANIC",
                        name="Copper Hydroxide 77% WP",
                        description="Copper-based bactericide",
                        dosage="2.0 g/L water",
                        application_timing="At disease onset",
                        safety_precautions=["Avoid use in high rainfall areas", "Do not mix with other chemicals"],
                        estimated_cost=350
                    )
                ],
                "preventive_measures": [
                    PreventiveMeasure(measure="Use certified healthy seeds", timing="At sowing"),
                    PreventiveMeasure(measure="Avoid excessive nitrogen", timing="Throughout"),
                    PreventiveMeasure(measure="Drain excess water", timing="During disease"),
                    PreventiveMeasure(measure="Remove infected debris", timing="After harvest")
                ],
                "kvk_expert_contact": "Contact your local KVK for expert consultation"
            },
            "powdery_mildew": {
                "disease_name": "Powdery Mildew",
                "disease_name_local": "चूर्णिल आसिता",
                "treatment_options": [
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Sulphur 80% WDG",
                        description="Contact fungicide for powdery mildew",
                        dosage="2.5 g/L water",
                        application_timing="At disease onset, repeat every 10-15 days",
                        safety_precautions=["Do not use when temperature >35°C", "Wear mask during application"],
                        estimated_cost=250
                    ),
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Hexaconazole 5% SC",
                        description="Systemic triazole fungicide",
                        dosage="2.0 mL/L water",
                        application_timing="At disease onset",
                        safety_precautions=["Avoid spray before rain", "Wear protective clothing"],
                        estimated_cost=450
                    ),
                    TreatmentOption(
                        type="ORGANIC",
                        name="Baking Soda Solution",
                        description="Home remedy for powdery mildew",
                        dosage="5 g/L water + 5 mL liquid soap",
                        application_timing="Weekly spray",
                        safety_precautions=["Safe for edible crops"],
                        estimated_cost=100
                    )
                ],
                "preventive_measures": [
                    PreventiveMeasure(measure="Proper plant spacing", timing="At planting"),
                    PreventiveMeasure(measure="Avoid overhead irrigation", timing="Throughout"),
                    PreventiveMeasure(measure="Remove infected leaves", timing="Regular"),
                    PreventiveMeasure(measure="Use resistant varieties", timing="At sowing")
                ],
                "kvk_expert_contact": "Contact your local KVK for expert consultation"
            },
            "late_blight": {
                "disease_name": "Late Blight",
                "disease_name_local": "पछेती झुलसा",
                "treatment_options": [
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Metalaxyl 8% + Mancozeb 64% WP",
                        description="Systemic and contact fungicide",
                        dosage="2.5 g/L water",
                        application_timing="At disease onset, repeat every 7-10 days",
                        safety_precautions=["Wear protective clothing", "Do not apply after 4 PM"],
                        estimated_cost=600
                    ),
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Cymoxanil 8% + Mancozeb 64% WP",
                        description="Curative fungicide",
                        dosage="2.0 g/L water",
                        application_timing="At disease onset",
                        safety_precautions=["Follow pre-harvest interval", "Wear gloves"],
                        estimated_cost=700
                    ),
                    TreatmentOption(
                        type="ORGANIC",
                        name="Copper Hydroxide 77% WP",
                        description="Copper-based protectant fungicide",
                        dosage="2.0 g/L water",
                        application_timing="Preventive spray every 10 days",
                        safety_precautions=["Avoid use in acidic soils"],
                        estimated_cost=350
                    )
                ],
                "preventive_measures": [
                    PreventiveMeasure(measure="Use healthy seed tubers", timing="At planting"),
                    PreventiveMeasure(measure="Hill potatoes", timing="During growth"),
                    PreventiveMeasure(measure="Destroy volunteer plants", timing="After harvest"),
                    PreventiveMeasure(measure="Avoid overhead irrigation", timing="Throughout")
                ],
                "kvk_expert_contact": "Contact your local KVK for expert consultation"
            },
            "rust": {
                "disease_name": "Rust",
                "disease_name_local": "गेरुआ रोग",
                "treatment_options": [
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Propiconazole 25% EC",
                        description="Systemic triazole fungicide",
                        dosage="1.0 mL/L water",
                        application_timing="At disease onset",
                        safety_precautions=["Wear protective equipment", "Do not apply to flowering crops"],
                        estimated_cost=550
                    ),
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Tebuconazole 250% EC",
                        description="Systemic fungicide",
                        dosage="1.5 mL/L water",
                        application_timing="At disease onset",
                        safety_precautions=["Follow label instructions", "Avoid drift to non-target areas"],
                        estimated_cost=500
                    ),
                    TreatmentOption(
                        type="ORGANIC",
                        name="Neem Cake Extract",
                        description="Organic treatment for rust",
                        dosage="50 g/L water",
                        application_timing="Weekly spray",
                        safety_precautions=["Safe for beneficial insects"],
                        estimated_cost=150
                    )
                ],
                "preventive_measures": [
                    PreventiveMeasure(measure="Use resistant varieties", timing="At sowing"),
                    PreventiveMeasure(measure="Remove alternate hosts", timing="Regular"),
                    PreventiveMeasure(measure="Avoid excessive nitrogen", timing="Throughout"),
                    PreventiveMeasure(measure="Proper crop rotation", timing="Seasonal")
                ],
                "kvk_expert_contact": "Contact your local KVK for expert consultation"
            },
            "wilt": {
                "disease_name": "Wilt",
                "disease_name_local": "मुरझाया रोग",
                "treatment_options": [
                    TreatmentOption(
                        type="CHEMICAL",
                        name="Carbendazim 50% WP",
                        description="Systemic fungicide",
                        dosage="1.0 g/L water",
                        application_timing="Soil drench at disease onset",
                        safety_precautions=["Wear gloves", "Do not mix with other chemicals"],
                        estimated_cost=400
                    ),
                    TreatmentOption(
                        type="ORGANIC",
                        name="Trichoderma viride",
                        description="Biological control agent",
                        dosage="5 g/L water as soil drench",
                        application_timing="At planting and 30 days after"),
                        safety_precautions=["Store in cool place", "Use before expiry"],
                        estimated_cost=200
                    )
                ],
                "preventive_measures": [
                    PreventiveMeasure(measure="Use resistant varieties", timing="At sowing"),
                    PreventiveMeasure(measure="Solarize soil", timing="Before planting"),
                    PreventiveMeasure(measure="Crop rotation", timing="Seasonal", notes="3-4 year rotation with non-host crops"),
                    PreventiveMeasure(measure="Use healthy seeds", timing="At sowing")
                ],
                "kvk_expert_contact": "Contact your local KVK for expert consultation"
            }
        }

    async def load_model(self) -> None:
        """Load the disease detection model."""
        try:
            # In a real implementation, this would load a pre-trained model
            # For now, we'll use a mock implementation
            self._model_loaded = True
            logger.info("Disease detection model loaded successfully")
        except Exception as e:
            logger.error(f"Failed to load disease detection model: {e}")
            raise

    async def unload_model(self) -> None:
        """Unload the model to free resources."""
        self._model = None
        self._model_loaded = False
        logger.info("Disease detection model unloaded")

    def is_model_loaded(self) -> bool:
        """Check if the model is loaded."""
        return self._model_loaded

    def _preprocess_image(self, image_data: str) -> Image.Image:
        """
        Preprocess image for model inference.
        
        Args:
            image_data: Base64 encoded image data
            
        Returns:
            Preprocessed PIL Image
        """
        # Decode base64 image
        image_bytes = base64.b64decode(image_data)
        image = Image.open(io.BytesIO(image_bytes))
        
        # Convert to RGB if necessary
        if image.mode != "RGB":
            image = image.convert("RGB")
        
        # Resize to model input size
        image = image.resize((224, 224))
        
        return image

    def _calculate_severity(self, confidence: float, affected_area: float) -> str:
        """
        Calculate severity level based on confidence and affected area.
        
        Args:
            confidence: Detection confidence score (0-100)
            affected_area: Percentage of affected area
            
        Returns:
            Severity level: LOW, MEDIUM, HIGH, or CRITICAL
        """
        # Combined score
        combined_score = (confidence * 0.6) + (affected_area * 0.4)
        
        if combined_score >= 80:
            return "CRITICAL"
        elif combined_score >= 60:
            return "HIGH"
        elif combined_score >= 40:
            return "MEDIUM"
        else:
            return "LOW"

    def _generate_bounding_boxes(self, disease_name: str, confidence: float) -> List[BoundingBox]:
        """
        Generate bounding boxes for affected regions.
        
        Args:
            disease_name: Name of the detected disease
            confidence: Detection confidence
            
        Returns:
            List of bounding boxes
        """
        # In a real implementation, this would come from the model's output
        # For now, return a placeholder
        return [
            BoundingBox(
                x=0.1,
                y=0.1,
                width=0.3,
                height=0.3,
                confidence=confidence / 100
            )
        ]

    async def _run_inference(self, image: Image.Image) -> List[dict]:
        """
        Run model inference on the image.
        
        Args:
            image: Preprocessed PIL Image
            
        Returns:
            List of detection results
        """
        # In a real implementation, this would use the loaded model
        # For now, return mock results based on image analysis
        
        # Simulate inference time
        time.sleep(0.1)
        
        # Mock detections - in production, this would come from the model
        mock_detections = [
            {
                "disease_name": "blast",
                "confidence": 85.5,
                "affected_area": 15.2
            }
        ]
        
        return mock_detections

    def _get_treatment_recommendation(self, disease_name: str) -> Optional[TreatmentRecommendation]:
        """
        Get treatment recommendations for a disease.
        
        Args:
            disease_name: Name of the disease
            
        Returns:
            TreatmentRecommendation or None if not found
        """
        disease_info = self._treatment_db.get(disease_name.lower())
        
        if not disease_info:
            return None
        
        # Calculate total cost
        total_cost = sum(
            option.estimated_cost 
            for option in disease_info["treatment_options"]
        )
        
        return TreatmentRecommendation(
            disease_name=disease_info["disease_name"],
            disease_name_local=disease_info.get("disease_name_local"),
            treatment_options=disease_info["treatment_options"],
            preventive_measures=disease_info["preventive_measures"],
            estimated_total_cost=total_cost,
            kvk_expert_contact=disease_info.get("kvk_expert_contact")
        )

    async def detect(
        self, 
        request: DiseaseDetectionRequest
    ) -> DiseaseDetectionResponse:
        """
        Detect diseases in a crop image.
        
        Args:
            request: Disease detection request with image data
            
        Returns:
            DiseaseDetectionResponse with detection results and recommendations
        """
        start_time = time.time()
        
        # Generate detection ID
        detection_id = str(uuid.uuid4())
        
        # Preprocess image
        image = self._preprocess_image(request.image_data)
        
        # Run inference
        detections = await self._run_inference(image)
        
        # Process detections
        results = []
        treatment_recommendations = []
        
        for detection in detections:
            disease_name = detection["disease_name"]
            confidence = detection["confidence"]
            affected_area = detection["affected_area"]
            
            # Calculate severity
            severity = self._calculate_severity(confidence, affected_area)
            
            # Generate bounding boxes
            bounding_boxes = self._generate_bounding_boxes(disease_name, confidence)
            
            # Create detection result
            result = DiseaseDetectionResult(
                disease_name=disease_name,
                disease_name_local=self._treatment_db.get(disease_name, {}).get("disease_name_local"),
                confidence_score=confidence,
                severity_level=severity,
                affected_area_percent=affected_area,
                bounding_boxes=bounding_boxes,
                is_healthy=False
            )
            results.append(result)
            
            # Get treatment recommendation
            treatment = self._get_treatment_recommendation(disease_name)
            if treatment:
                treatment_recommendations.append(treatment)
        
        # Sort detections by confidence
        results.sort(key=lambda x: x.confidence_score, reverse=True)
        
        # Calculate processing time
        processing_time_ms = (time.time() - start_time) * 1000
        
        # Determine if healthy
        is_healthy = len(results) == 0 or all(
            r.confidence_score < self.CONFIDENCE_THRESHOLD 
            for r in results
        )
        
        # Generate message based on results
        message = None
        if is_healthy:
            message = "Your crop appears healthy. If you suspect a disease, please upload a clearer image focusing on the affected area."
        elif len(results) == 0:
            message = "No disease was detected with sufficient confidence. Please upload a clearer image."
        
        # Generate image path
        image_path = f"/uploads/disease_detection/{request.user_id}/{detection_id}.jpg"
        
        return DiseaseDetectionResponse(
            detection_id=detection_id,
            user_id=request.user_id,
            crop_id=request.crop_id,
            detections=results,
            treatment_recommendations=treatment_recommendations,
            image_path=image_path,
            model_version=self.MODEL_VERSION,
            processing_time_ms=processing_time_ms,
            is_healthy=is_healthy,
            message=message,
            timestamp=datetime.utcnow()
        )

    def apply_confidence_threshold(self, results: List[DiseaseDetectionResult]) -> List[DiseaseDetectionResult]:
        """
        Filter detection results based on confidence threshold.
        
        Args:
            results: List of detection results
            
        Returns:
            Filtered list with confidence >= threshold
        """
        return [
            r for r in results 
            if r.confidence_score >= self.CONFIDENCE_THRESHOLD
        ]

    def get_treatment_for_disease(self, disease_name: str) -> Optional[TreatmentRecommendation]:
        """
        Get treatment recommendations for a specific disease.
        
        Args:
            disease_name: Name of the disease
            
        Returns:
            TreatmentRecommendation or None
        """
        return self._get_treatment_recommendation(disease_name)

    def get_supported_diseases(self) -> List[str]:
        """Get list of supported diseases."""
        return list(self._treatment_db.keys())

    def get_supported_crops(self) -> List[str]:
        """Get list of supported crops."""
        return self.SUPPORTED_CROPS


# Global disease detection service instance
disease_detection_service = DiseaseDetectionService()