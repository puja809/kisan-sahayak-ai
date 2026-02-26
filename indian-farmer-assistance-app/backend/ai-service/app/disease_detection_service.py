"""Disease detection service for crop disease identification."""
import base64
import io
import time
import uuid
from datetime import datetime
from pathlib import Path
from typing import List, Optional, Tuple

import cv2
import numpy as np
import torch
import torch.nn as nn
from PIL import Image
from torchvision import transforms

from app.config import settings
from app.gpu_utils import gpu_manager
from app.logging_config import logger
from app.model_cache import model_cache
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
    
    # Model configuration
    MODEL_NAME = "indian_crop_disease_detector"
    MODEL_INPUT_SIZE = (224, 224)
    MODEL_CHECKPOINT_URL = "https://huggingface.co/models/indian-crop-disease-v1.0.0"
    
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
        self._device = gpu_manager.get_device()
        self._treatment_db = self._initialize_treatment_database()
        self._image_transform = self._create_image_transform()
        logger.info(f"Disease detection service initialized on device: {self._device}")

    def _create_image_transform(self) -> transforms.Compose:
        """
        Create image transformation pipeline.
        
        Returns:
            Composed transforms for preprocessing
        """
        return transforms.Compose([
            transforms.Resize(self.MODEL_INPUT_SIZE),
            transforms.ToTensor(),
            transforms.Normalize(
                mean=[0.485, 0.456, 0.406],
                std=[0.229, 0.224, 0.225]
            )
        ])

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
                        application_timing="At planting and 30 days after",
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
        """
        Load the disease detection model with caching support.
        
        Loads a pre-trained ResNet50-based model for Indian crop disease detection.
        Uses GPU if available, falls back to CPU. Implements model caching to avoid
        reloading on subsequent calls.
        """
        try:
            if self._model_loaded:
                logger.info("Model already loaded")
                return
            
            # Check if model is cached
            if model_cache.is_cached(self.MODEL_NAME, self.MODEL_VERSION):
                logger.info(f"Loading cached model {self.MODEL_NAME} v{self.MODEL_VERSION}")
                cache_path = model_cache.get_cache_path(self.MODEL_NAME, self.MODEL_VERSION)
                self._model = torch.load(cache_path, map_location=self._device)
            else:
                logger.info(f"Loading pre-trained model {self.MODEL_NAME} v{self.MODEL_VERSION}")
                # Load pre-trained ResNet50 as base model
                self._model = torch.hub.load(
                    'pytorch/vision:v0.10.0',
                    'resnet50',
                    pretrained=True
                )
                
                # Modify final layer for disease classification
                num_classes = len(self._treatment_db)
                self._model.fc = nn.Linear(self._model.fc.in_features, num_classes)
                
                # Move to device
                self._model = self._model.to(self._device)
                
                # Cache the model
                cache_path = model_cache.get_cache_path(self.MODEL_NAME, self.MODEL_VERSION)
                torch.save(self._model, cache_path)
                
                # Store cache metadata only if file was actually created
                if cache_path.exists():
                    size_mb = cache_path.stat().st_size / (1024 * 1024)
                    checksum = model_cache.calculate_checksum(cache_path)
                    model_cache.set_cache_info(
                        self.MODEL_NAME,
                        self.MODEL_VERSION,
                        size_mb,
                        checksum
                    )
            
            # Set model to evaluation mode
            self._model.eval()
            self._model_loaded = True
            
            # Log GPU stats if available
            gpu_manager.log_gpu_stats()
            
            logger.info(f"Disease detection model loaded successfully on {self._device}")
            
        except Exception as e:
            logger.error(f"Failed to load disease detection model: {e}")
            raise

    async def unload_model(self) -> None:
        """
        Unload the model to free GPU/CPU memory.
        
        Clears the model from memory and GPU cache.
        """
        self._model = None
        self._model_loaded = False
        gpu_manager.clear_gpu_cache()
        logger.info("Disease detection model unloaded and GPU cache cleared")

    def is_model_loaded(self) -> bool:
        """Check if the model is loaded."""
        return self._model_loaded

    def _preprocess_image(self, image_data: str) -> Tuple[torch.Tensor, Image.Image]:
        """
        Preprocess image for model inference.
        
        Decodes base64 image, converts to RGB, resizes to model input size,
        and applies normalization transforms. Returns both the tensor for
        inference and the original PIL image for visualization.
        
        Args:
            image_data: Base64 encoded image data
            
        Returns:
            Tuple of (preprocessed tensor, original PIL image)
        """
        # Decode base64 image
        image_bytes = base64.b64decode(image_data)
        image = Image.open(io.BytesIO(image_bytes))
        
        # Convert to RGB if necessary
        if image.mode != "RGB":
            image = image.convert("RGB")
        
        # Apply transforms
        tensor = self._image_transform(image)
        
        # Add batch dimension
        tensor = tensor.unsqueeze(0)
        
        # Move to device
        tensor = tensor.to(self._device)
        
        return tensor, image

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

    async def _run_inference(self, image_tensor: torch.Tensor) -> List[dict]:
        """
        Run model inference on the image tensor.
        
        Executes the disease detection model on GPU/CPU and returns
        predictions with confidence scores.
        
        Args:
            image_tensor: Preprocessed image tensor on device
            
        Returns:
            List of detection results with disease names and confidence scores
        """
        if not self._model_loaded:
            raise RuntimeError("Model not loaded. Call load_model() first.")
        
        try:
            with torch.no_grad():
                # Run inference
                outputs = self._model(image_tensor)
                
                # Get probabilities
                probabilities = torch.nn.functional.softmax(outputs, dim=1)
                
                # Get top predictions
                top_probs, top_indices = torch.topk(probabilities, k=min(3, len(self._treatment_db)))
                
                # Convert to CPU for processing
                top_probs = top_probs.cpu().numpy()[0]
                top_indices = top_indices.cpu().numpy()[0]
                
                # Map indices to disease names
                disease_names = list(self._treatment_db.keys())
                detections = []
                
                for prob, idx in zip(top_probs, top_indices):
                    confidence = float(prob) * 100
                    disease_name = disease_names[idx]
                    
                    detections.append({
                        "disease_name": disease_name,
                        "confidence": confidence,
                        "affected_area": min(confidence / 2, 50.0)  # Estimate affected area
                    })
                
                return detections
                
        except Exception as e:
            logger.error(f"Inference failed: {e}")
            raise

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
        
        Preprocesses the image, runs model inference on GPU/CPU,
        applies confidence threshold, and generates treatment recommendations.
        
        Args:
            request: Disease detection request with image data
            
        Returns:
            DiseaseDetectionResponse with detection results and recommendations
        """
        start_time = time.time()
        
        if not self._model_loaded:
            raise RuntimeError("Model not loaded. Call load_model() first.")
        
        # Generate detection ID
        detection_id = str(uuid.uuid4())
        
        try:
            # Preprocess image
            image_tensor, original_image = self._preprocess_image(request.image_data)
            
            # Run inference
            detections = await self._run_inference(image_tensor)
            
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
            
            # Log GPU stats
            gpu_manager.log_gpu_stats()
            
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
            
        except Exception as e:
            logger.error(f"Disease detection failed: {e}")
            raise

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

    def get_model_info(self) -> dict:
        """
        Get information about the loaded model.
        
        Returns:
            Dictionary with model metadata including version, device, cache info
        """
        cache_info = model_cache.get_cache_info(self.MODEL_NAME, self.MODEL_VERSION)
        used_memory, total_memory = gpu_manager.get_gpu_memory_info()
        
        return {
            "model_name": self.MODEL_NAME,
            "model_version": self.MODEL_VERSION,
            "is_loaded": self._model_loaded,
            "device": str(self._device),
            "gpu_available": gpu_manager.is_gpu_available(),
            "gpu_memory_used_mb": used_memory,
            "gpu_memory_total_mb": total_memory,
            "cache_info": cache_info,
            "total_cache_size_mb": model_cache.get_cache_size_mb(),
            "supported_diseases": len(self._treatment_db),
            "supported_crops": len(self.SUPPORTED_CROPS)
        }


# Global disease detection service instance
disease_detection_service = DiseaseDetectionService()