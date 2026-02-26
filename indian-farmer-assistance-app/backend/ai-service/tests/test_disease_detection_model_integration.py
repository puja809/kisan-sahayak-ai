"""Tests for disease detection model integration with GPU support and caching."""
import asyncio
import base64
import io
import pytest
import torch
from pathlib import Path
from PIL import Image
from unittest.mock import Mock, patch, AsyncMock

from app.disease_detection_service import DiseaseDetectionService
from app.gpu_utils import GPUManager
from app.model_cache import ModelCache
from app.models import DiseaseDetectionRequest


class TestModelCache:
    """Test model caching functionality."""

    def test_cache_path_generation(self):
        """Test that cache paths are generated correctly."""
        cache = ModelCache()
        path = cache.get_cache_path("test_model", "1.0.0")
        assert "test_model_v1.0.0" in str(path)

    def test_cache_existence_check(self):
        """Test checking if model is cached."""
        cache = ModelCache()
        assert not cache.is_cached("nonexistent", "1.0.0")

    def test_cache_metadata_storage(self):
        """Test storing and retrieving cache metadata."""
        cache = ModelCache()
        cache.set_cache_info("test_model", "1.0.0", 100.5, "abc123")
        
        info = cache.get_cache_info("test_model", "1.0.0")
        assert info is not None
        assert info["size_mb"] == 100.5
        assert info["checksum"] == "abc123"

    def test_cache_size_calculation(self):
        """Test calculating total cache size."""
        cache = ModelCache()
        size = cache.get_cache_size_mb()
        assert isinstance(size, float)
        assert size >= 0


class TestGPUManager:
    """Test GPU management functionality."""

    def test_gpu_availability_check(self):
        """Test checking GPU availability."""
        available = GPUManager.is_gpu_available()
        assert isinstance(available, bool)

    def test_device_selection(self):
        """Test device selection (GPU or CPU)."""
        device = GPUManager.get_device()
        assert isinstance(device, torch.device)
        assert device.type in ["cuda", "cpu"]

    def test_gpu_memory_info(self):
        """Test GPU memory information retrieval."""
        used, total = GPUManager.get_gpu_memory_info()
        assert isinstance(used, float)
        assert isinstance(total, float)
        assert used >= 0
        assert total >= 0

    def test_gpu_cache_clearing(self):
        """Test GPU cache clearing."""
        # Should not raise exception
        GPUManager.clear_gpu_cache()


class TestDiseaseDetectionModelIntegration:
    """Test disease detection model integration."""

    @pytest.fixture
    def service(self):
        """Create disease detection service instance."""
        return DiseaseDetectionService()

    @pytest.fixture
    def sample_image_base64(self):
        """Create a sample image in base64 format."""
        # Create a simple RGB image
        img = Image.new("RGB", (224, 224), color="red")
        img_bytes = io.BytesIO()
        img.save(img_bytes, format="PNG")
        img_bytes.seek(0)
        return base64.b64encode(img_bytes.getvalue()).decode()

    def test_service_initialization(self, service):
        """Test service initialization."""
        assert service is not None
        assert not service.is_model_loaded()
        assert service._device is not None

    def test_image_transform_creation(self, service):
        """Test image transform pipeline creation."""
        transform = service._image_transform
        assert transform is not None
        assert len(transform.transforms) > 0

    @pytest.mark.asyncio
    async def test_model_loading(self, service):
        """Test model loading with GPU support."""
        with patch("torch.hub.load") as mock_load:
            # Mock the model
            mock_model = Mock()
            mock_model.fc = Mock()
            mock_model.fc.in_features = 2048
            mock_load.return_value = mock_model
            
            with patch("torch.save"):
                await service.load_model()
            
            assert service.is_model_loaded()

    @pytest.mark.asyncio
    async def test_model_unloading(self, service):
        """Test model unloading."""
        with patch("torch.hub.load") as mock_load:
            mock_model = Mock()
            mock_model.fc = Mock()
            mock_model.fc.in_features = 2048
            mock_load.return_value = mock_model
            
            with patch("torch.save"):
                await service.load_model()
            
            await service.unload_model()
            assert not service.is_model_loaded()

    def test_image_preprocessing(self, service, sample_image_base64):
        """Test image preprocessing pipeline."""
        tensor, original_image = service._preprocess_image(sample_image_base64)
        
        assert isinstance(tensor, torch.Tensor)
        assert tensor.shape[0] == 1  # Batch dimension
        assert tensor.shape[1] == 3  # RGB channels
        assert tensor.shape[2] == 224  # Height
        assert tensor.shape[3] == 224  # Width
        assert original_image is not None

    def test_severity_calculation(self, service):
        """Test severity level calculation."""
        # Test different confidence and affected area combinations
        severity_critical = service._calculate_severity(90, 80)
        assert severity_critical == "CRITICAL"
        
        severity_high = service._calculate_severity(70, 50)
        assert severity_high == "HIGH"
        
        severity_medium = service._calculate_severity(50, 30)
        assert severity_medium == "MEDIUM"
        
        severity_low = service._calculate_severity(30, 10)
        assert severity_low == "LOW"

    def test_bounding_box_generation(self, service):
        """Test bounding box generation."""
        boxes = service._generate_bounding_boxes("blast", 85.0)
        
        assert len(boxes) > 0
        assert all(0 <= box.x <= 1 for box in boxes)
        assert all(0 <= box.y <= 1 for box in boxes)
        assert all(0 <= box.width <= 1 for box in boxes)
        assert all(0 <= box.height <= 1 for box in boxes)

    def test_treatment_recommendation_retrieval(self, service):
        """Test treatment recommendation retrieval."""
        treatment = service._get_treatment_recommendation("blast")
        
        assert treatment is not None
        assert treatment.disease_name == "Rice Blast"
        assert len(treatment.treatment_options) > 0
        assert len(treatment.preventive_measures) > 0

    def test_confidence_threshold_application(self, service):
        """Test confidence threshold filtering."""
        from app.models import DiseaseDetectionResult, BoundingBox
        
        results = [
            DiseaseDetectionResult(
                disease_name="blast",
                disease_name_local="धान का ब्लास्ट",
                confidence_score=85.0,
                severity_level="HIGH",
                affected_area_percent=20.0,
                bounding_boxes=[],
                is_healthy=False
            ),
            DiseaseDetectionResult(
                disease_name="rust",
                disease_name_local="गेरुआ रोग",
                confidence_score=65.0,
                severity_level="MEDIUM",
                affected_area_percent=15.0,
                bounding_boxes=[],
                is_healthy=False
            )
        ]
        
        filtered = service.apply_confidence_threshold(results)
        assert len(filtered) == 1
        assert filtered[0].confidence_score >= 70.0

    def test_supported_crops_list(self, service):
        """Test supported crops list."""
        crops = service.get_supported_crops()
        
        assert len(crops) > 0
        assert "paddy" in crops
        assert "wheat" in crops
        assert "tomato" in crops

    def test_supported_diseases_list(self, service):
        """Test supported diseases list."""
        diseases = service.get_supported_diseases()
        
        assert len(diseases) > 0
        assert "blast" in diseases
        assert "powdery_mildew" in diseases

    def test_model_info_retrieval(self, service):
        """Test model information retrieval."""
        info = service.get_model_info()
        
        assert "model_name" in info
        assert "model_version" in info
        assert "is_loaded" in info
        assert "device" in info
        assert "gpu_available" in info
        assert "supported_diseases" in info
        assert "supported_crops" in info

    @pytest.mark.asyncio
    async def test_disease_detection_with_mock_model(self, service, sample_image_base64):
        """Test disease detection with mocked model."""
        # Mock the model
        with patch("torch.hub.load") as mock_load:
            mock_model = Mock()
            mock_model.fc = Mock()
            mock_model.fc.in_features = 2048
            mock_model.eval = Mock()
            
            # Mock inference output
            mock_output = torch.tensor([[0.1, 0.85, 0.05]])  # High confidence for disease 1
            mock_model.return_value = mock_output
            mock_load.return_value = mock_model
            
            with patch("torch.save"):
                await service.load_model()
            
            # Create detection request
            request = DiseaseDetectionRequest(
                user_id="test_user",
                crop_id="test_crop",
                image_data=sample_image_base64,
                filename="test.png",
                content_type="image/png"
            )
            
            # Mock the inference
            with patch.object(service, "_run_inference") as mock_inference:
                mock_inference.return_value = [
                    {
                        "disease_name": "blast",
                        "confidence": 85.0,
                        "affected_area": 20.0
                    }
                ]
                
                response = await service.detect(request)
                
                assert response.detection_id is not None
                assert response.user_id == "test_user"
                assert len(response.detections) > 0
                assert response.model_version == "1.0.0"

    def test_model_version_consistency(self, service):
        """Test that model version is consistent."""
        assert service.MODEL_VERSION == "1.0.0"
        info = service.get_model_info()
        assert info["model_version"] == "1.0.0"

    def test_device_consistency(self, service):
        """Test that device is consistent."""
        device = service._device
        info = service.get_model_info()
        assert str(device) in info["device"]
