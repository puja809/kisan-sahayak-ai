"""Unit tests for image validation service."""
import base64
import io
from unittest.mock import MagicMock, patch

import pytest
from PIL import Image

from app.image_validation_service import ImageValidationService
from app.models import ImageValidationRequest, ImageValidationResponse


class TestImageValidationService:
    """Tests for the ImageValidationService class."""

    @pytest.fixture
    def image_validation_service(self):
        """Create an image validation service instance."""
        return ImageValidationService()

    @pytest.fixture
    def sample_image_bytes(self):
        """Create sample image bytes for testing."""
        img = Image.new("RGB", (500, 500), color="green")
        buffer = io.BytesIO()
        img.save(buffer, format="JPEG")
        return buffer.getvalue()

    @pytest.fixture
    def sample_image_base64(self, sample_image_bytes):
        """Create base64 encoded sample image."""
        return base64.b64encode(sample_image_bytes).decode("utf-8")

    def test_service_initialization(self, image_validation_service):
        """Test that the service initializes correctly."""
        assert image_validation_service.SUPPORTED_FORMATS == {"image/jpeg", "image/png"}
        assert image_validation_service.MAX_FILE_SIZE == 10 * 1024 * 1024
        assert image_validation_service.MIN_WIDTH == 224
        assert image_validation_service.MIN_HEIGHT == 224

    def test_validate_format_jpeg(self, image_validation_service):
        """Test JPEG format validation."""
        is_valid, error = image_validation_service.validate_format("image/jpeg")
        assert is_valid is True
        assert error is None

    def test_validate_format_png(self, image_validation_service):
        """Test PNG format validation."""
        is_valid, error = image_validation_service.validate_format("image/png")
        assert is_valid is True
        assert error is None

    def test_validate_format_case_insensitive(self, image_validation_service):
        """Test format validation is case insensitive."""
        is_valid, error = image_validation_service.validate_format("IMAGE/JPEG")
        assert is_valid is True

    def test_validate_format_unsupported(self, image_validation_service):
        """Test unsupported format validation."""
        is_valid, error = image_validation_service.validate_format("image/gif")
        assert is_valid is False
        assert "Unsupported image format" in error

    def test_validate_format_webp(self, image_validation_service):
        """Test WebP format is rejected."""
        is_valid, error = image_validation_service.validate_format("image/webp")
        assert is_valid is False
        assert "Unsupported image format" in error

    def test_validate_size_under_limit(self, image_validation_service):
        """Test file size validation under limit."""
        is_valid, error = image_validation_service.validate_size(5 * 1024 * 1024)  # 5MB
        assert is_valid is True
        assert error is None

    def test_validate_size_at_limit(self, image_validation_service):
        """Test file size validation at exact limit."""
        is_valid, error = image_validation_service.validate_size(10 * 1024 * 1024)  # 10MB
        assert is_valid is True
        assert error is None

    def test_validate_size_over_limit(self, image_validation_service):
        """Test file size validation over limit."""
        is_valid, error = image_validation_service.validate_size(15 * 1024 * 1024)  # 15MB
        assert is_valid is False
        assert "exceeds maximum allowed size" in error

    def test_validate_dimensions_valid(self, image_validation_service):
        """Test valid image dimensions."""
        is_valid, warnings = image_validation_service.validate_dimensions(500, 500)
        assert is_valid is True
        assert len(warnings) == 0

    def test_validate_dimensions_minimum(self, image_validation_service):
        """Test minimum valid image dimensions."""
        is_valid, warnings = image_validation_service.validate_dimensions(224, 224)
        assert is_valid is True

    def test_validate_dimensions_below_minimum(self, image_validation_service):
        """Test dimensions below minimum."""
        is_valid, warnings = image_validation_service.validate_dimensions(100, 100)
        assert is_valid is False
        assert "too small" in warnings[0]

    def test_validate_dimensions_very_large(self, image_validation_service):
        """Test very large dimensions generate warning."""
        is_valid, warnings = image_validation_service.validate_dimensions(5000, 5000)
        assert is_valid is True
        assert len(warnings) > 0
        assert "very large" in warnings[0]

    def test_validate_request_valid(self, image_validation_service):
        """Test full validation request with valid data."""
        request = ImageValidationRequest(
            filename="test.jpg",
            file_size=5 * 1024 * 1024,
            content_type="image/jpeg"
        )
        response = image_validation_service.validate(request)
        assert response.is_valid is True
        assert len(response.errors) == 0

    def test_validate_request_invalid_format(self, image_validation_service):
        """Test validation request with invalid format."""
        request = ImageValidationRequest(
            filename="test.gif",
            file_size=5 * 1024 * 1024,
            content_type="image/gif"
        )
        response = image_validation_service.validate(request)
        assert response.is_valid is False
        assert len(response.errors) > 0

    def test_validate_request_too_large(self, image_validation_service):
        """Test validation request with file too large."""
        request = ImageValidationRequest(
            filename="test.jpg",
            file_size=15 * 1024 * 1024,
            content_type="image/jpeg"
        )
        response = image_validation_service.validate(request)
        assert response.is_valid is False
        assert len(response.errors) > 0

    def test_validate_with_content_valid(self, image_validation_service, sample_image_base64):
        """Test validation with actual image content."""
        response = image_validation_service.validate_with_content(
            image_data=sample_image_base64,
            filename="test.jpg",
            content_type="image/jpeg"
        )
        assert response.is_valid is True

    def test_validate_with_content_invalid_format(self, image_validation_service, sample_image_base64):
        """Test validation with invalid content type."""
        response = image_validation_service.validate_with_content(
            image_data=sample_image_base64,
            filename="test.jpg",
            content_type="image/gif"
        )
        assert response.is_valid is False
        assert len(response.errors) > 0

    def test_validate_with_content_empty_data(self, image_validation_service):
        """Test validation with empty image data."""
        response = image_validation_service.validate_with_content(
            image_data="",
            filename="test.jpg",
            content_type="image/jpeg"
        )
        assert response.is_valid is False

    def test_validate_with_content_corrupted_image(self, image_validation_service):
        """Test validation with corrupted image data."""
        response = image_validation_service.validate_with_content(
            image_data="not-valid-base64-image-data!!!",
            filename="test.jpg",
            content_type="image/jpeg"
        )
        assert response.is_valid is False

    def test_detect_blur_clear_image(self, image_validation_service):
        """Test blur detection on a clear image."""
        # Create a sharp image with clear edges
        img = Image.new("RGB", (500, 500), color="white")
        # Add some edges
        for i in range(100, 400):
            for j in range(100, 400):
                img.putpixel((i, j), (0, 0, 0))
        
        variance, is_blurry = image_validation_service.detect_blur(img)
        assert variance > ImageValidationService.BLUR_THRESHOLD
        assert is_blurry is False

    def test_detect_blur_blurry_image(self, image_validation_service):
        """Test blur detection on a blurry image."""
        # Create a blurry image by adding noise
        img = Image.new("RGB", (500, 500), color=(128, 128, 128))
        
        variance, is_blurry = image_validation_service.detect_blur(img)
        assert is_blurry is True

    def test_check_lighting_good(self, image_validation_service):
        """Test lighting check with good lighting."""
        # Create an image with good lighting
        img = Image.new("RGB", (500, 500), color=(128, 128, 128))
        
        brightness, contrast, is_poor = image_validation_service.check_lighting(img)
        assert brightness > ImageValidationService.MIN_BRIGHTNESS
        assert brightness < ImageValidationService.MAX_BRIGHTNESS
        assert is_poor is False

    def test_check_lighting_too_dark(self, image_validation_service):
        """Test lighting check with too dark image."""
        img = Image.new("RGB", (500, 500), color=(10, 10, 10))
        
        brightness, contrast, is_poor = image_validation_service.check_lighting(img)
        assert brightness < ImageValidationService.MIN_BRIGHTNESS
        assert is_poor is True

    def test_check_lighting_too_bright(self, image_validation_service):
        """Test lighting check with too bright image."""
        img = Image.new("RGB", (500, 500), color=(250, 250, 250))
        
        brightness, contrast, is_poor = image_validation_service.check_lighting(img)
        assert brightness > ImageValidationService.MAX_BRIGHTNESS
        assert is_poor is True

    def test_check_lighting_low_contrast(self, image_validation_service):
        """Test lighting check with low contrast."""
        # Create an image with uniform color (low contrast)
        img = Image.new("RGB", (500, 500), color=(128, 128, 128))
        
        brightness, contrast, is_poor = image_validation_service.check_lighting(img)
        assert contrast < 10
        assert is_poor is True


class TestImageValidationEdgeCases:
    """Edge case tests for image validation."""

    @pytest.fixture
    def image_validation_service(self):
        """Create an image validation service instance."""
        return ImageValidationService()

    def test_empty_content_type(self, image_validation_service):
        """Test validation with empty content type."""
        is_valid, error = image_validation_service.validate_format("")
        assert is_valid is False

    def test_none_content_type(self, image_validation_service):
        """Test validation with None content type."""
        is_valid, error = image_validation_service.validate_format(None)
        assert is_valid is False

    def test_zero_file_size(self, image_validation_service):
        """Test validation with zero file size."""
        is_valid, error = image_validation_service.validate_size(0)
        assert is_valid is True

    def test_negative_file_size(self, image_validation_service):
        """Test validation with negative file size."""
        is_valid, error = image_validation_service.validate_size(-100)
        assert is_valid is True  # Negative becomes 0

    def test_validate_dimensions_zero(self, image_validation_service):
        """Test validation with zero dimensions."""
        is_valid, warnings = image_validation_service.validate_dimensions(0, 0)
        assert is_valid is False

    def test_validate_dimensions_negative(self, image_validation_service):
        """Test validation with negative dimensions."""
        is_valid, warnings = image_validation_service.validate_dimensions(-100, -100)
        assert is_valid is False