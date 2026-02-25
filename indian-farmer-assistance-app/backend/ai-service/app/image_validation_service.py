"""Image validation service for disease detection."""
import base64
import io
import math
from typing import List, Optional, Tuple

from PIL import Image

from app.logging_config import logger
from app.models import ImageValidationRequest, ImageValidationResponse


class ImageValidationService:
    """Service for validating images for disease detection."""

    # Supported image formats
    SUPPORTED_FORMATS = {"image/jpeg", "image/png"}
    
    # Maximum file size (10MB)
    MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB in bytes
    
    # Minimum image dimensions
    MIN_WIDTH = 224
    MIN_HEIGHT = 224
    
    # Maximum image dimensions (for reasonable processing)
    MAX_WIDTH = 4096
    MAX_HEIGHT = 4096
    
    # Blur detection threshold (Laplacian variance)
    BLUR_THRESHOLD = 50.0
    
    # Lighting thresholds
    MIN_BRIGHTNESS = 30
    MAX_BRIGHTNESS = 240

    def __init__(self):
        """Initialize the image validation service."""
        pass

    def validate_format(self, content_type: str) -> Tuple[bool, Optional[str]]:
        """
        Validate that the image format is supported.
        
        Args:
            content_type: The MIME type of the image
            
        Returns:
            Tuple of (is_valid, error_message)
        """
        if content_type.lower() not in self.SUPPORTED_FORMATS:
            return False, f"Unsupported image format: {content_type}. Supported formats: JPEG, PNG"
        return True, None

    def validate_size(self, file_size: int) -> Tuple[bool, Optional[str]]:
        """
        Validate that the image size is within limits.
        
        Args:
            file_size: The file size in bytes
            
        Returns:
            Tuple of (is_valid, error_message)
        """
        if file_size > self.MAX_FILE_SIZE:
            max_size_mb = self.MAX_FILE_SIZE / (1024 * 1024)
            actual_size_mb = file_size / (1024 * 1024)
            return False, f"Image size ({actual_size_mb:.1f}MB) exceeds maximum allowed size ({max_size_mb:.1f}MB)"
        return True, None

    def validate_dimensions(self, width: int, height: int) -> Tuple[bool, List[str]]:
        """
        Validate that the image dimensions are within acceptable range.
        
        Args:
            width: Image width in pixels
            height: Image height in pixels
            
        Returns:
            Tuple of (is_valid, list of warnings)
        """
        warnings = []
        
        if width < self.MIN_WIDTH or height < self.MIN_HEIGHT:
            return False, [f"Image dimensions ({width}x{height}) are too small. Minimum: {self.MIN_WIDTH}x{self.MIN_HEIGHT}"]
        
        if width > self.MAX_WIDTH or height > self.MAX_HEIGHT:
            warnings.append(f"Image dimensions ({width}x{height}) are very large and may cause processing issues")
        
        return True, warnings

    def detect_blur(self, image: Image.Image) -> Tuple[float, bool]:
        """
        Detect if the image is blurry using Laplacian variance.
        
        Args:
            image: PIL Image object
            
        Returns:
            Tuple of (laplacian_variance, is_blurry)
        """
        # Convert to grayscale
        gray = image.convert("L")
        
        # Calculate Laplacian
        import numpy as np
        img_array = np.array(gray)
        
        # Compute Laplacian variance
        laplacian = np.var(img_array)
        
        is_blurry = laplacian < self.BLUR_THRESHOLD
        
        return laplacian, is_blurry

    def check_lighting(self, image: Image.Image) -> Tuple[float, float, bool]:
        """
        Check image lighting conditions.
        
        Args:
            image: PIL Image object
            
        Returns:
            Tuple of (brightness, contrast, is_poor_lighting)
        """
        import numpy as np
        
        # Convert to grayscale
        gray = image.convert("L")
        img_array = np.array(gray)
        
        # Calculate brightness (mean pixel value)
        brightness = np.mean(img_array)
        
        # Calculate contrast (standard deviation)
        contrast = np.std(img_array)
        
        # Check for poor lighting
        is_poor_lighting = (
            brightness < self.MIN_BRIGHTNESS or 
            brightness > self.MAX_BRIGHTNESS or
            contrast < 10  # Very low contrast
        )
        
        return brightness, contrast, is_poor_lighting

    def validate_image_content(self, image_data: str) -> Tuple[bool, List[str]]:
        """
        Validate the actual image content for quality issues.
        
        Args:
            image_data: Base64 encoded image data
            
        Returns:
            Tuple of (is_valid, list of warnings)
        """
        warnings = []
        
        try:
            # Decode base64 image
            image_bytes = base64.b64decode(image_data)
            image = Image.open(io.BytesIO(image_bytes))
            
            # Check dimensions
            width, height = image.size
            is_valid, dim_warnings = self.validate_dimensions(width, height)
            if not is_valid:
                return False, dim_warnings
            warnings.extend(dim_warnings)
            
            # Check for blur
            laplacian_variance, is_blurry = self.detect_blur(image)
            if is_blurry:
                warnings.append("Image may be blurry. Please ensure the image is in focus for accurate disease detection")
            
            # Check lighting
            brightness, contrast, is_poor_lighting = self.check_lighting(image)
            if is_poor_lighting:
                if brightness < self.MIN_BRIGHTNESS:
                    warnings.append("Image is too dark. Please take the photo in better lighting conditions")
                elif brightness > self.MAX_BRIGHTNESS:
                    warnings.append("Image is too bright. Please avoid direct sunlight")
                else:
                    warnings.append("Image has low contrast. Please ensure even lighting")
            
            return True, warnings
            
        except Exception as e:
            logger.error(f"Error validating image content: {e}")
            return False, [f"Failed to process image: {str(e)}"]

    def validate(self, request: ImageValidationRequest) -> ImageValidationResponse:
        """
        Validate an image for disease detection.
        
        Args:
            request: Image validation request with filename, file_size, and content_type
            
        Returns:
            ImageValidationResponse with validation results
        """
        errors = []
        warnings = []
        
        # Validate format
        is_valid, format_error = self.validate_format(request.content_type)
        if not is_valid:
            errors.append(format_error)
        
        # Validate size
        is_valid, size_error = self.validate_size(request.file_size)
        if not is_valid:
            errors.append(size_error)
        
        # If basic validations pass, validate content
        if not errors:
            # Note: Full content validation requires the actual image data
            # This is done separately in the detection endpoint
            pass
        
        return ImageValidationResponse(
            is_valid=len(errors) == 0,
            errors=errors,
            warnings=warnings
        )

    def validate_with_content(
        self, 
        image_data: str, 
        filename: str, 
        content_type: str
    ) -> ImageValidationResponse:
        """
        Validate an image including content analysis.
        
        Args:
            image_data: Base64 encoded image data
            filename: Original filename
            content_type: MIME type
            
        Returns:
            ImageValidationResponse with validation results
        """
        errors = []
        warnings = []
        
        # Validate format
        is_valid, format_error = self.validate_format(content_type)
        if not is_valid:
            errors.append(format_error)
        
        # Validate size
        try:
            file_size = len(base64.b64decode(image_data)) if image_data else 0
        except Exception:
            file_size = 0
        
        is_valid, size_error = self.validate_size(file_size)
        if not is_valid:
            errors.append(size_error)
        
        # Validate content if no format/size errors
        if not errors and image_data:
            is_valid, content_warnings = self.validate_image_content(image_data)
            if not is_valid:
                errors.extend(content_warnings)
            else:
                warnings.extend(content_warnings)
        
        return ImageValidationResponse(
            is_valid=len(errors) == 0,
            errors=errors,
            warnings=warnings
        )


# Global image validation service instance
image_validation_service = ImageValidationService()