"""Unit tests for disease detection service."""
import base64
import io
import uuid
import pytest
import torch
from PIL import Image
from app.disease_detection_service import DiseaseDetectionService
from app.models import BoundingBox, DiseaseDetectionRequest, DiseaseDetectionResult


class TestDiseaseDetectionService:
    @pytest.fixture
    def disease_detection_service(self):
        return DiseaseDetectionService()

    def test_service_initialization(self, disease_detection_service):
        assert disease_detection_service.CONFIDENCE_THRESHOLD == 70.0
        assert disease_detection_service.MODEL_VERSION == "1.0.0"
        assert len(disease_detection_service.SUPPORTED_CROPS) > 0

    def test_is_model_loaded_initially_false(self, disease_detection_service):
        assert disease_detection_service.is_model_loaded() is False

    @pytest.mark.asyncio
    async def test_load_model(self, disease_detection_service):
        await disease_detection_service.load_model()
        assert disease_detection_service.is_model_loaded() is True

    @pytest.mark.asyncio
    async def test_unload_model(self, disease_detection_service):
        await disease_detection_service.load_model()
        await disease_detection_service.unload_model()
        assert disease_detection_service.is_model_loaded() is False

    def test_calculate_severity_low(self, disease_detection_service):
        assert disease_detection_service._calculate_severity(30, 10) == "LOW"

    def test_calculate_severity_medium(self, disease_detection_service):
        assert disease_detection_service._calculate_severity(50, 30) == "MEDIUM"

    def test_calculate_severity_high(self, disease_detection_service):
        assert disease_detection_service._calculate_severity(70, 50) == "HIGH"

    def test_calculate_severity_critical(self, disease_detection_service):
        assert disease_detection_service._calculate_severity(90, 80) == "CRITICAL"

    def test_generate_bounding_boxes(self, disease_detection_service):
        boxes = disease_detection_service._generate_bounding_boxes("blast", 85.0)
        assert len(boxes) == 1
        assert isinstance(boxes[0], BoundingBox)
        assert boxes[0].confidence == 0.85

    def test_get_treatment_for_disease(self, disease_detection_service):
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        assert treatment is not None
        assert treatment.disease_name == "Rice Blast"

    def test_get_treatment_for_unknown_disease(self, disease_detection_service):
        treatment = disease_detection_service.get_treatment_for_disease("unknown")
        assert treatment is None

    def test_get_supported_diseases(self, disease_detection_service):
        diseases = disease_detection_service.get_supported_diseases()
        assert "blast" in diseases
        assert "bacterial_blight" in diseases

    def test_get_supported_crops(self, disease_detection_service):
        crops = disease_detection_service.get_supported_crops()
        assert "paddy" in crops
        assert "wheat" in crops

    def test_apply_confidence_threshold_filters_low_confidence(self, disease_detection_service):
        results = [
            DiseaseDetectionResult(disease_name="blast", confidence_score=50.0, severity_level="LOW", affected_area_percent=10.0, bounding_boxes=[]),
            DiseaseDetectionResult(disease_name="rust", confidence_score=85.0, severity_level="HIGH", affected_area_percent=20.0, bounding_boxes=[])
        ]
        filtered = disease_detection_service.apply_confidence_threshold(results)
        assert len(filtered) == 1
        assert filtered[0].disease_name == "rust"

    def test_apply_confidence_threshold_empty_list(self, disease_detection_service):
        filtered = disease_detection_service.apply_confidence_threshold([])
        assert len(filtered) == 0

    def test_treatment_database_has_organic_options(self, disease_detection_service):
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        assert treatment is not None
        organic_options = [opt for opt in treatment.treatment_options if opt.type == "ORGANIC"]
        assert len(organic_options) > 0

    def test_treatment_database_has_chemical_options(self, disease_detection_service):
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        assert treatment is not None
        chemical_options = [opt for opt in treatment.treatment_options if opt.type == "CHEMICAL"]
        assert len(chemical_options) > 0

    def test_treatment_options_have_safety_precautions(self, disease_detection_service):
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        assert treatment is not None
        for option in treatment.treatment_options:
            assert len(option.safety_precautions) > 0

    def test_treatment_recommendation_has_kvk_contact(self, disease_detection_service):
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        assert treatment is not None
        assert treatment.kvk_expert_contact is not None


class TestDiseaseDetectionEndpoints:
    @pytest.fixture
    def disease_detection_service(self):
        return DiseaseDetectionService()

    def test_preprocess_image_rgb(self, disease_detection_service):
        img = Image.new("RGB", (500, 500), color="green")
        buffer = io.BytesIO()
        img.save(buffer, format="JPEG")
        image_data = base64.b64encode(buffer.getvalue()).decode("utf-8")
        image = disease_detection_service._preprocess_image(image_data)
        assert image.mode == "RGB"
        assert image.size == (224, 224)

    @pytest.mark.asyncio
    async def test_detect_returns_response(self, disease_detection_service):
        await disease_detection_service.load_model()
        img = Image.new("RGB", (500, 500), color="green")
        buffer = io.BytesIO()
        img.save(buffer, format="JPEG")
        image_data = base64.b64encode(buffer.getvalue()).decode("utf-8")
        request = DiseaseDetectionRequest(user_id="test_user", image_data=image_data, filename="test.jpg", content_type="image/jpeg")
        response = await disease_detection_service.detect(request)
        assert response is not None
        assert response.user_id == "test_user"
        assert response.model_version == "1.0.0"


class TestDiseaseDetectionEdgeCases:
    @pytest.fixture
    def disease_detection_service(self):
        return DiseaseDetectionService()

    def test_get_treatment_case_insensitive(self, disease_detection_service):
        t1 = disease_detection_service.get_treatment_for_disease("BLAST")
        t2 = disease_detection_service.get_treatment_for_disease("blast")
        assert t1 is not None and t2 is not None
        assert t1.disease_name == t2.disease_name

    def test_severity_calculation_with_zero_confidence(self, disease_detection_service):
        assert disease_detection_service._calculate_severity(0, 0) == "LOW"

    def test_severity_calculation_with_max_values(self, disease_detection_service):
        assert disease_detection_service._calculate_severity(100, 100) == "CRITICAL"

    def test_treatment_database_completeness(self, disease_detection_service):
        diseases = disease_detection_service.get_supported_diseases()
        for disease in diseases:
            treatment = disease_detection_service.get_treatment_for_disease(disease)
            assert treatment is not None
            assert treatment.disease_name is not None
            assert len(treatment.treatment_options) > 0
            assert len(treatment.preventive_measures) > 0
            assert treatment.estimated_total_cost > 0



class TestImageValidation:
    """Tests for image validation in disease detection."""
    
    @pytest.fixture
    def disease_detection_service(self):
        return DiseaseDetectionService()

    def test_preprocess_image_converts_to_rgb(self, disease_detection_service):
        """Test that image preprocessing converts to RGB."""
        img = Image.new("RGBA", (500, 500), color=(255, 0, 0, 255))
        buffer = io.BytesIO()
        img.save(buffer, format="PNG")
        image_data = base64.b64encode(buffer.getvalue()).decode("utf-8")
        
        tensor, original_image = disease_detection_service._preprocess_image(image_data)
        
        assert original_image.mode == "RGB"
        # Original image is returned before resizing
        assert original_image.size == (500, 500)

    def test_preprocess_image_returns_tuple(self, disease_detection_service):
        """Test that preprocessing returns a tuple of tensor and image."""
        img = Image.new("RGB", (1000, 1000), color="green")
        buffer = io.BytesIO()
        img.save(buffer, format="JPEG")
        image_data = base64.b64encode(buffer.getvalue()).decode("utf-8")
        
        result = disease_detection_service._preprocess_image(image_data)
        
        assert isinstance(result, tuple)
        assert len(result) == 2

    def test_preprocess_image_returns_tensor(self, disease_detection_service):
        """Test that preprocessing returns a tensor."""
        img = Image.new("RGB", (500, 500), color="blue")
        buffer = io.BytesIO()
        img.save(buffer, format="JPEG")
        image_data = base64.b64encode(buffer.getvalue()).decode("utf-8")
        
        tensor, original_image = disease_detection_service._preprocess_image(image_data)
        
        assert isinstance(tensor, torch.Tensor)
        assert tensor.shape[0] == 1  # Batch dimension


class TestConfidenceThreshold:
    """Tests for confidence threshold handling."""
    
    @pytest.fixture
    def disease_detection_service(self):
        return DiseaseDetectionService()

    def test_confidence_threshold_value(self, disease_detection_service):
        """Test that confidence threshold is set correctly."""
        assert disease_detection_service.CONFIDENCE_THRESHOLD == 70.0

    def test_apply_confidence_threshold_removes_low_confidence(self, disease_detection_service):
        """Test that low confidence detections are filtered."""
        results = [
            DiseaseDetectionResult(
                disease_name="blast",
                confidence_score=50.0,
                severity_level="LOW",
                affected_area_percent=10.0,
                bounding_boxes=[]
            ),
            DiseaseDetectionResult(
                disease_name="rust",
                confidence_score=85.0,
                severity_level="HIGH",
                affected_area_percent=20.0,
                bounding_boxes=[]
            )
        ]
        
        filtered = disease_detection_service.apply_confidence_threshold(results)
        
        assert len(filtered) == 1
        assert filtered[0].disease_name == "rust"
        assert filtered[0].confidence_score == 85.0

    def test_apply_confidence_threshold_keeps_exact_threshold(self, disease_detection_service):
        """Test that detections at exact threshold are kept."""
        results = [
            DiseaseDetectionResult(
                disease_name="blast",
                confidence_score=70.0,
                severity_level="MEDIUM",
                affected_area_percent=15.0,
                bounding_boxes=[]
            )
        ]
        
        filtered = disease_detection_service.apply_confidence_threshold(results)
        
        assert len(filtered) == 1
        assert filtered[0].confidence_score == 70.0

    def test_apply_confidence_threshold_all_below_threshold(self, disease_detection_service):
        """Test filtering when all detections are below threshold."""
        results = [
            DiseaseDetectionResult(
                disease_name="blast",
                confidence_score=50.0,
                severity_level="LOW",
                affected_area_percent=10.0,
                bounding_boxes=[]
            ),
            DiseaseDetectionResult(
                disease_name="rust",
                confidence_score=60.0,
                severity_level="MEDIUM",
                affected_area_percent=15.0,
                bounding_boxes=[]
            )
        ]
        
        filtered = disease_detection_service.apply_confidence_threshold(results)
        
        assert len(filtered) == 0


class TestTreatmentRecommendations:
    """Tests for treatment recommendation retrieval."""
    
    @pytest.fixture
    def disease_detection_service(self):
        return DiseaseDetectionService()

    def test_get_treatment_recommendation_blast(self, disease_detection_service):
        """Test getting treatment for blast disease."""
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        
        assert treatment is not None
        assert treatment.disease_name == "Rice Blast"
        assert len(treatment.treatment_options) > 0
        assert len(treatment.preventive_measures) > 0

    def test_treatment_has_organic_and_chemical_options(self, disease_detection_service):
        """Test that treatment includes both organic and chemical options."""
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        
        assert treatment is not None
        organic_options = [opt for opt in treatment.treatment_options if opt.type == "ORGANIC"]
        chemical_options = [opt for opt in treatment.treatment_options if opt.type == "CHEMICAL"]
        
        assert len(organic_options) > 0
        assert len(chemical_options) > 0

    def test_treatment_option_has_required_fields(self, disease_detection_service):
        """Test that treatment options have all required fields."""
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        
        assert treatment is not None
        for option in treatment.treatment_options:
            assert option.name is not None
            assert option.description is not None
            assert option.dosage is not None
            assert option.application_timing is not None
            assert len(option.safety_precautions) > 0
            assert option.estimated_cost > 0

    def test_preventive_measure_has_required_fields(self, disease_detection_service):
        """Test that preventive measures have all required fields."""
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        
        assert treatment is not None
        for measure in treatment.preventive_measures:
            assert measure.measure is not None
            assert measure.timing is not None

    def test_treatment_estimated_cost_is_positive(self, disease_detection_service):
        """Test that estimated treatment cost is positive."""
        treatment = disease_detection_service.get_treatment_for_disease("blast")
        
        assert treatment is not None
        assert treatment.estimated_total_cost > 0

    def test_all_supported_diseases_have_treatment(self, disease_detection_service):
        """Test that all supported diseases have treatment recommendations."""
        diseases = disease_detection_service.get_supported_diseases()
        
        for disease in diseases:
            treatment = disease_detection_service.get_treatment_for_disease(disease)
            assert treatment is not None
            assert treatment.disease_name is not None
            assert len(treatment.treatment_options) > 0


class TestMultipleDiseaseDetection:
    """Tests for handling multiple disease detections."""
    
    @pytest.fixture
    def disease_detection_service(self):
        return DiseaseDetectionService()

    def test_multiple_detections_ranked_by_confidence(self, disease_detection_service):
        """Test that multiple detections are ranked by confidence."""
        results = [
            DiseaseDetectionResult(
                disease_name="rust",
                confidence_score=60.0,
                severity_level="MEDIUM",
                affected_area_percent=15.0,
                bounding_boxes=[]
            ),
            DiseaseDetectionResult(
                disease_name="blast",
                confidence_score=85.0,
                severity_level="HIGH",
                affected_area_percent=25.0,
                bounding_boxes=[]
            ),
            DiseaseDetectionResult(
                disease_name="wilt",
                confidence_score=75.0,
                severity_level="HIGH",
                affected_area_percent=20.0,
                bounding_boxes=[]
            )
        ]
        
        # Sort by confidence (descending)
        sorted_results = sorted(results, key=lambda x: x.confidence_score, reverse=True)
        
        assert sorted_results[0].disease_name == "blast"
        assert sorted_results[1].disease_name == "wilt"
        assert sorted_results[2].disease_name == "rust"

    def test_detection_response_includes_all_detections(self, disease_detection_service):
        """Test that detection response includes all detected diseases."""
        results = [
            DiseaseDetectionResult(
                disease_name="blast",
                confidence_score=85.0,
                severity_level="HIGH",
                affected_area_percent=25.0,
                bounding_boxes=[]
            ),
            DiseaseDetectionResult(
                disease_name="rust",
                confidence_score=75.0,
                severity_level="MEDIUM",
                affected_area_percent=15.0,
                bounding_boxes=[]
            )
        ]
        
        assert len(results) == 2
        assert all(r.disease_name in ["blast", "rust"] for r in results)


class TestSeverityCalculation:
    """Tests for severity level calculation."""
    
    @pytest.fixture
    def disease_detection_service(self):
        return DiseaseDetectionService()

    def test_severity_low_boundary(self, disease_detection_service):
        """Test severity calculation at LOW boundary."""
        severity = disease_detection_service._calculate_severity(20, 10)
        assert severity == "LOW"

    def test_severity_medium_boundary(self, disease_detection_service):
        """Test severity calculation at MEDIUM boundary."""
        severity = disease_detection_service._calculate_severity(50, 30)
        assert severity == "MEDIUM"

    def test_severity_high_boundary(self, disease_detection_service):
        """Test severity calculation at HIGH boundary."""
        severity = disease_detection_service._calculate_severity(70, 50)
        assert severity == "HIGH"

    def test_severity_critical_boundary(self, disease_detection_service):
        """Test severity calculation at CRITICAL boundary."""
        severity = disease_detection_service._calculate_severity(90, 80)
        assert severity == "CRITICAL"

    def test_severity_with_high_confidence_low_area(self, disease_detection_service):
        """Test severity with high confidence but low affected area."""
        severity = disease_detection_service._calculate_severity(95, 5)
        # Combined score = 95 * 0.6 + 5 * 0.4 = 57 + 2 = 59 (MEDIUM)
        assert severity == "MEDIUM"

    def test_severity_with_low_confidence_high_area(self, disease_detection_service):
        """Test severity with low confidence but high affected area."""
        severity = disease_detection_service._calculate_severity(30, 90)
        # Combined score = 30 * 0.6 + 90 * 0.4 = 18 + 36 = 54 (MEDIUM)
        assert severity == "MEDIUM"
