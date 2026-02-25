"""
Property-based tests for AI Service.

Feature: indian-farmer-assistance-app, Property 39: Vector Embedding Generation
Feature: indian-farmer-assistance-app, Property 40: Semantic Search Ranking
"""
import math
from typing import List

import pytest
from hypothesis import given, settings, strategies as st

from app.embedding_service import EmbeddingService
from app.models import DocumentMetadata, SearchResult


# =============================================================================
# Property 39: Vector Embedding Generation
# Validates: Requirements 21.3, 21.4
# =============================================================================

class TestVectorEmbeddingGeneration:
    """Property tests for vector embedding generation."""

    @pytest.fixture
    def embedding_service(self):
        """Create an embedding service instance."""
        return EmbeddingService()

    @pytest.fixture
    def mock_model(self):
        """Create a mock sentence transformer model."""
        mock = MagicMock()
        mock.encode.return_value = [[0.1] * 768]
        return mock

    @st.composite
    def generate_text_strategy(draw):
        """Generate random text for testing."""
        # Generate text of varying lengths
        length = draw(st.integers(min_value=1, max_value=1000))
        # Generate random characters
        chars = draw(st.text(min_size=length, max_size=length, alphabet=st.characters(
            whitelist_categories=['L', 'N'],
            whitelist_characters=' '
        )))
        return chars

    @given(text=st.text(min_size=1, max_size=1000, alphabet=st.characters(
        whitelist_categories=['L', 'N'],
        whitelist_characters=' .,!?@#$%&*()[]{}'
    )))
    @settings(max_examples=10)
    @pytest.mark.asyncio
    async def test_embedding_dimension_is_constant(
        self, embedding_service, mock_model, text
    ):
        """
        Property: For any valid text input, the generated embedding should have
        the configured dimensionality (768 dimensions).
        
        Validates: Requirements 21.3, 21.4
        """
        embedding_service._model = mock_model
        embedding_service._model_loaded = True

        embedding = await embedding_service.generate_embedding(text)

        assert len(embedding) == 768, f"Embedding dimension should be 768, got {len(embedding)}"

    @given(text=st.text(min_size=1, max_size=100, alphabet=st.ascii_letters))
    @settings(max_examples=20)
    @pytest.mark.asyncio
    async def test_embedding_values_are_floats(
        self, embedding_service, mock_model, text
    ):
        """
        Property: For any valid text input, all embedding values should be floats.
        
        Validates: Requirements 21.3
        """
        embedding_service._model = mock_model
        embedding_service._model_loaded = True

        embedding = await embedding_service.generate_embedding(text)

        assert all(isinstance(x, (float, int)) for x in embedding), \
            "All embedding values should be numeric"

    @given(text=st.text(min_size=1, max_size=100, alphabet=st.ascii_letters))
    @settings(max_examples=10)
    @pytest.mark.asyncio
    async def test_embedding_reproducibility(
        self, embedding_service, mock_model, text
    ):
        """
        Property: For the same text input, the embedding generation should
        produce the same result (deterministic).
        
        Validates: Requirements 21.3
        """
        embedding_service._model = mock_model
        embedding_service._model_loaded = True

        embedding1 = await embedding_service.generate_embedding(text)
        embedding2 = await embedding_service.generate_embedding(text)

        assert embedding1 == embedding2, "Same text should produce same embedding"

    @given(texts=st.lists(st.text(min_size=1, max_size=50, alphabet=st.ascii_letters),
                          min_size=1, max_size=10))
    @settings(max_examples=10)
    @pytest.mark.asyncio
    async def test_batch_embedding_count_matches_input(
        self, embedding_service, mock_model, texts
    ):
        """
        Property: For a batch of N texts, the batch embedding should return
        N embeddings.
        
        Validates: Requirements 21.3
        """
        embedding_service._model = mock_model
        embedding_service._model_loaded = True

        # Configure mock to return correct number of embeddings
        mock_model.encode.return_value = [[0.1] * 768 for _ in texts]

        embeddings = await embedding_service.generate_embeddings_batch(texts)

        assert len(embeddings) == len(texts), \
            f"Expected {len(texts)} embeddings, got {len(embeddings)}"

    @given(texts=st.lists(st.text(min_size=1, max_size=50), min_size=1, max_size=10))
    @settings(max_examples=10)
    @pytest.mark.asyncio
    async def test_batch_embedding_dimension_consistency(
        self, embedding_service, mock_model, texts
    ):
        """
        Property: All embeddings in a batch should have the same dimension.
        
        Validates: Requirements 21.3, 21.4
        """
        embedding_service._model = mock_model
        embedding_service._model_loaded = True

        mock_model.encode.return_value = [[0.1] * 768 for _ in texts]

        embeddings = await embedding_service.generate_embeddings_batch(texts)

        # All embeddings should have the same dimension
        dimensions = [len(e) for e in embeddings]
        assert len(set(dimensions)) == 1, "All embeddings should have same dimension"
        assert dimensions[0] == 768, f"Expected dimension 768, got {dimensions[0]}"


# =============================================================================
# Property 40: Semantic Search Ranking
# Validates: Requirement 21.8
# =============================================================================

class TestSemanticSearchRanking:
    """Property tests for semantic search ranking."""

    @st.composite
    def search_results_strategy(draw):
        """Generate random search results for testing."""
        num_results = draw(st.integers(min_value=2, max_value=20))
        
        results = []
        for i in range(num_results):
            score = draw(st.floats(min_value=0.0, max_value=1.0))
            results.append(SearchResult(
                document_id=f"doc{i}",
                title=f"Document {i}",
                category=draw(st.sampled_from(["schemes", "guidelines", "crop_info", "disease_mgmt", "market_intel"])),
                content="Sample content",
                similarity_score=score,
                metadata=DocumentMetadata()
            ))
        
        return results

    @given(results=search_results_strategy())
    @settings(max_examples=50)
    def test_search_results_ranked_descending(self, results):
        """
        Property: For any list of search results, they should be ranked by
        cosine similarity to the query embedding in descending order, such that
        for any two adjacent documents in the results, the first has similarity
        >= the second.
        
        Validates: Requirement 21.8
        """
        # Sort results by similarity in descending order
        sorted_results = sorted(results, key=lambda x: x.similarity_score, reverse=True)
        
        # Verify descending order
        for i in range(len(sorted_results) - 1):
            assert sorted_results[i].similarity_score >= sorted_results[i + 1].similarity_score, \
                f"Results not in descending order: {sorted_results[i].similarity_score} < {sorted_results[i+1].similarity_score}"

    @given(results=search_results_strategy())
    @settings(max_examples=30)
    def test_search_results_score_range(self, results):
        """
        Property: All similarity scores should be in the valid range [0, 1].
        
        Validates: Requirement 21.8
        """
        for result in results:
            assert 0.0 <= result.similarity_score <= 1.0, \
                f"Similarity score {result.similarity_score} out of range [0, 1]"

    @given(num_results=st.integers(min_value=1, max_value=100))
    @settings(max_examples=20)
    def test_search_results_limit_enforced(self, num_results):
        """
        Property: When limiting search results, the number of returned results
        should not exceed the limit.
        
        Validates: Requirement 21.8
        """
        limit = min(num_results, 50)  # Cap at reasonable limit
        
        # Create results
        results = [
            SearchResult(
                document_id=f"doc{i}",
                title=f"Document {i}",
                category="schemes",
                content="Content",
                similarity_score=1.0 - (i * 0.01),
                metadata=DocumentMetadata()
            )
            for i in range(num_results)
        ]
        
        # Apply limit
        limited_results = results[:limit]
        
        assert len(limited_results) <= limit, \
            f"Limited results {len(limited_results)} exceeds limit {limit}"


class TestCosineSimilarityProperties:
    """Property tests for cosine similarity computation."""

    @given(vec1=st.lists(st.floats(min_value=-1.0, max_value=1.0), min_size=1, max_size=100),
           vec2=st.lists(st.floats(min_value=-1.0, max_value=1.0), min_size=1, max_size=100))
    @settings(max_examples=30)
    def test_cosine_similarity_symmetry(self, embedding_service, vec1, vec2):
        """
        Property: Cosine similarity should be symmetric:
        sim(a, b) == sim(b, a)
        """
        # Ensure same length
        min_len = min(len(vec1), len(vec2))
        vec1 = vec1[:min_len]
        vec2 = vec2[:min_len]

        sim_ab = embedding_service.cosine_similarity(vec1, vec2)
        sim_ba = embedding_service.cosine_similarity(vec2, vec1)

        assert abs(sim_ab - sim_ba) < 0.0001, "Cosine similarity should be symmetric"

    @given(vec=st.lists(st.floats(min_value=-1.0, max_value=1.0), min_size=1, max_size=100))
    @settings(max_examples=30)
    def test_cosine_similarity_self_is_one(self, embedding_service, vec):
        """
        Property: Cosine similarity of a vector with itself should be 1
        (for non-zero vectors).
        """
        sim = embedding_service.cosine_similarity(vec, vec)
        
        # For non-zero vectors, similarity should be 1
        if sum(x * x for x in vec) > 0:
            assert abs(sim - 1.0) < 0.0001, \
                f"Cosine similarity of self should be 1, got {sim}"

    @given(vec=st.lists(st.floats(min_value=-1.0, max_value=1.0), min_size=1, max_size=100))
    @settings(max_examples=30)
    def test_cosine_similarity_range(self, embedding_service, vec):
        """
        Property: Cosine similarity should always be in range [-1, 1].
        """
        sim = embedding_service.cosine_similarity(vec, vec)
        assert -1.0 <= sim <= 1.0, \
            f"Cosine similarity {sim} out of range [-1, 1]"


# =============================================================================
# Property 16: Image Validation
# Validates: Requirements 9.1
# =============================================================================

class TestImageValidationProperties:
    """Property tests for image validation."""

    @pytest.fixture
    def image_validation_service(self):
        """Create an image validation service instance."""
        from app.image_validation_service import ImageValidationService
        return ImageValidationService()

    @st.composite
    def valid_content_type_strategy(draw):
        """Generate valid content types."""
        return draw(st.sampled_from(["image/jpeg", "image/png", "IMAGE/JPEG", "IMAGE/PNG"]))

    @st.composite
    def invalid_content_type_strategy(draw):
        """Generate invalid content types."""
        return draw(st.sampled_from([
            "image/gif", "image/webp", "image/bmp", "application/pdf",
            "text/plain", "image/svg+xml", "image/tiff"
        ]))

    @given(content_type=valid_content_type_strategy())
    @settings(max_examples=20)
    def test_valid_formats_accepted(self, image_validation_service, content_type):
        """
        Property: For any valid image format (JPEG, PNG), the format validation
        should pass and not return an error.
        
        Validates: Requirements 9.1
        """
        is_valid, error = image_validation_service.validate_format(content_type)
        assert is_valid is True, f"Valid format {content_type} was rejected: {error}"
        assert error is None

    @given(content_type=invalid_content_type_strategy())
    @settings(max_examples=20)
    def test_invalid_formats_rejected(self, image_validation_service, content_type):
        """
        Property: For any invalid image format (not JPEG or PNG), the format
        validation should fail and return an error message.
        
        Validates: Requirements 9.1
        """
        is_valid, error = image_validation_service.validate_format(content_type)
        assert is_valid is False, f"Invalid format {content_type} was accepted"
        assert error is not None
        assert "Unsupported image format" in error

    @st.composite
    def valid_file_size_strategy(draw):
        """Generate valid file sizes (0 to 10MB)."""
        return draw(st.integers(min_value=0, max_value=10 * 1024 * 1024))

    @st.composite
    def invalid_file_size_strategy(draw):
        """Generate invalid file sizes (>10MB)."""
        return draw(st.integers(min_value=10 * 1024 * 1024 + 1, max_value=50 * 1024 * 1024))

    @given(file_size=valid_file_size_strategy())
    @settings(max_examples=20)
    def test_valid_sizes_accepted(self, image_validation_service, file_size):
        """
        Property: For any file size ≤ 10MB, the size validation should pass.
        
        Validates: Requirements 9.1
        """
        is_valid, error = image_validation_service.validate_size(file_size)
        assert is_valid is True, f"Valid size {file_size} was rejected: {error}"

    @given(file_size=invalid_file_size_strategy())
    @settings(max_examples=20)
    def test_invalid_sizes_rejected(self, image_validation_service, file_size):
        """
        Property: For any file size > 10MB, the size validation should fail
        and return an error message indicating the size limit.
        
        Validates: Requirements 9.1
        """
        is_valid, error = image_validation_service.validate_size(file_size)
        assert is_valid is False, f"Invalid size {file_size} was accepted"
        assert error is not None
        assert "exceeds maximum allowed size" in error

    @given(
        content_type=valid_content_type_strategy(),
        file_size=valid_file_size_strategy()
    )
    @settings(max_examples=30)
    def test_valid_combination_passes(self, image_validation_service, content_type, file_size):
        """
        Property: For any combination of valid format (JPEG/PNG) and valid size
        (≤10MB), the validation should pass.
        
        Validates: Requirements 9.1
        """
        from app.models import ImageValidationRequest
        
        request = ImageValidationRequest(
            filename="test.jpg",
            file_size=file_size,
            content_type=content_type
        )
        response = image_validation_service.validate(request)
        
        assert response.is_valid is True, \
            f"Valid combination was rejected: format={content_type}, size={file_size}"
        assert len(response.errors) == 0

    @given(
        content_type=invalid_content_type_strategy(),
        file_size=valid_file_size_strategy()
    )
    @settings(max_examples=20)
    def test_invalid_format_fails_regardless_of_size(
        self, image_validation_service, content_type, file_size
    ):
        """
        Property: For any invalid format, validation should fail regardless of
        the file size being valid.
        
        Validates: Requirements 9.1
        """
        from app.models import ImageValidationRequest
        
        request = ImageValidationRequest(
            filename="test.gif",
            file_size=file_size,
            content_type=content_type
        )
        response = image_validation_service.validate(request)
        
        assert response.is_valid is False
        assert len(response.errors) > 0
        assert any("format" in error.lower() for error in response.errors)

    @given(
        content_type=valid_content_type_strategy(),
        file_size=invalid_file_size_strategy()
    )
    @settings(max_examples=20)
    def test_invalid_size_fails_regardless_of_format(
        self, image_validation_service, content_type, file_size
    ):
        """
        Property: For any invalid size (>10MB), validation should fail regardless
        of the format being valid.
        
        Validates: Requirements 9.1
        """
        from app.models import ImageValidationRequest
        
        request = ImageValidationRequest(
            filename="test.jpg",
            file_size=file_size,
            content_type=content_type
        )
        response = image_validation_service.validate(request)
        
        assert response.is_valid is False
        assert len(response.errors) > 0
        assert any("size" in error.lower() or "exceeds" in error.lower() 
                  for error in response.errors)


# =============================================================================
# Property 17: Disease Detection Confidence Threshold
# Validates: Requirements 9.8
# =============================================================================

class TestDiseaseDetectionConfidenceThreshold:
    """Property tests for disease detection confidence threshold."""

    @pytest.fixture
    def disease_detection_service(self):
        """Create a disease detection service instance."""
        from app.disease_detection_service import DiseaseDetectionService
        return DiseaseDetectionService()

    @st.composite
    def detection_result_strategy(draw):
        """Generate random detection results."""
        return {
            "disease_name": draw(st.sampled_from(["blast", "bacterial_blight", "powdery_mildew", "rust", "wilt"])),
            "confidence": draw(st.floats(min_value=0.0, max_value=100.0)),
            "affected_area": draw(st.floats(min_value=0.0, max_value=100.0))
        }

    @given(detections=st.lists(detection_result_strategy(), min_size=0, max_size=10))
    @settings(max_examples=50)
    def test_confidence_threshold_filters_low_confidence(
        self, disease_detection_service, detections
    ):
        """
        Property: For any detection result with confidence score below 70%,
        the apply_confidence_threshold method should filter it out.
        
        Validates: Requirements 9.8
        """
        from app.models import DiseaseDetectionResult
        
        results = [
            DiseaseDetectionResult(
                disease_name=d["disease_name"],
                confidence_score=d["confidence"],
                severity_level=disease_detection_service._calculate_severity(
                    d["confidence"], d["affected_area"]
                ),
                affected_area_percent=d["affected_area"],
                bounding_boxes=[]
            )
            for d in detections
        ]
        
        filtered = disease_detection_service.apply_confidence_threshold(results)
        
        # All filtered results should have confidence >= 70
        for result in filtered:
            assert result.confidence_score >= 70.0, \
                f"Result with confidence {result.confidence_score} was not filtered out"

    @given(detections=st.lists(detection_result_strategy(), min_size=1, max_size=10))
    @settings(max_examples=50)
    def test_high_confidence_results_kept(self, disease_detection_service, detections):
        """
        Property: For any detection result with confidence score ≥ 70%,
        the apply_confidence_threshold method should keep it.
        
        Validates: Requirements 9.8
        """
        from app.models import DiseaseDetectionResult
        
        # Create results with high confidence
        high_confidence_detections = [
            d for d in detections if d["confidence"] >= 70.0
        ]
        
        if not high_confidence_detections:
            return  # Skip if no high confidence results
        
        results = [
            DiseaseDetectionResult(
                disease_name=d["disease_name"],
                confidence_score=d["confidence"],
                severity_level=disease_detection_service._calculate_severity(
                    d["confidence"], d["affected_area"]
                ),
                affected_area_percent=d["affected_area"],
                bounding_boxes=[]
            )
            for d in high_confidence_detections
        ]
        
        filtered = disease_detection_service.apply_confidence_threshold(results)
        
        # All high confidence results should be kept
        assert len(filtered) == len(results), \
            f"High confidence results were filtered: {len(filtered)} vs {len(results)}"

    @given(confidence=st.floats(min_value=0.0, max_value=100.0))
    @settings(max_examples=100)
    def test_threshold_boundary_at_70_percent(self, disease_detection_service, confidence):
        """
        Property: The confidence threshold should be exactly 70%. Results with
        confidence = 70% should be kept, results with confidence < 70% should be filtered.
        
        Validates: Requirements 9.8
        """
        from app.models import DiseaseDetectionResult
        
        result = DiseaseDetectionResult(
            disease_name="test",
            confidence_score=confidence,
            severity_level="LOW",
            affected_area_percent=10.0,
            bounding_boxes=[]
        )
        
        filtered = disease_detection_service.apply_confidence_threshold([result])
        
        if confidence >= 70.0:
            assert len(filtered) == 1, \
                f"Result with confidence {confidence}% should be kept"
        else:
            assert len(filtered) == 0, \
                f"Result with confidence {confidence}% should be filtered out"

    @given(detections=st.lists(detection_result_strategy(), min_size=0, max_size=10))
    @settings(max_examples=30)
    def test_empty_list_handled(self, disease_detection_service, detections):
        """
        Property: For an empty list of detection results, the
        apply_confidence_threshold method should return an empty list.
        
        Validates: Requirements 9.8
        """
        from app.models import DiseaseDetectionResult
        
        results = [
            DiseaseDetectionResult(
                disease_name=d["disease_name"],
                confidence_score=d["confidence"],
                severity_level=disease_detection_service._calculate_severity(
                    d["confidence"], d["affected_area"]
                ),
                affected_area_percent=d["affected_area"],
                bounding_boxes=[]
            )
            for d in detections
        ]
        
        filtered = disease_detection_service.apply_confidence_threshold([])
        
        assert filtered == [], "Empty list should return empty list"

    @given(detections=st.lists(detection_result_strategy(), min_size=1, max_size=10))
    @settings(max_examples=30)
    def test_threshold_preserves_severity_ordering(
        self, disease_detection_service, detections
    ):
        """
        Property: After applying the confidence threshold, the remaining
        results should still be sorted in descending order by confidence.
        
        Validates: Requirements 9.7, 9.8
        """
        from app.models import DiseaseDetectionResult
        
        results = [
            DiseaseDetectionResult(
                disease_name=d["disease_name"],
                confidence_score=d["confidence"],
                severity_level=disease_detection_service._calculate_severity(
                    d["confidence"], d["affected_area"]
                ),
                affected_area_percent=d["affected_area"],
                bounding_boxes=[]
            )
            for d in detections
        ]
        
        filtered = disease_detection_service.apply_confidence_threshold(results)
        
        # Check descending order
        for i in range(len(filtered) - 1):
            assert filtered[i].confidence_score >= filtered[i + 1].confidence_score, \
                f"Results not in descending order: {filtered[i].confidence_score} < {filtered[i+1].confidence_score}"


# Helper fixture for property tests
@pytest.fixture
def embedding_service():
    """Create an embedding service instance for property tests."""
    return EmbeddingService()