"""Pytest configuration and shared fixtures."""
import sys
from pathlib import Path

import pytest

# Add the app directory to the path
app_path = Path(__file__).parent.parent
sys.path.insert(0, str(app_path))


@pytest.fixture(scope="session")
def event_loop_policy():
    """Use the default event loop policy for async tests."""
    import asyncio
    return asyncio.DefaultEventLoopPolicy()


@pytest.fixture
def mock_mongodb():
    """Create a mock MongoDB client."""
    from unittest.mock import MagicMock
    from motor.motor_asyncio import AsyncIOMotorClient
    
    mock_client = MagicMock(spec=AsyncIOMotorClient)
    return mock_client


@pytest.fixture
def sample_documents():
    """Create sample documents for testing."""
    from app.models import Document, DocumentMetadata
    from datetime import datetime
    
    return [
        Document(
            document_id="doc1",
            title="PM-Kisan Scheme",
            category="schemes",
            content="PM-Kisan Samman Nidhi provides ₹6000 per year to farmer families.",
            content_language="en",
            embedding=[0.1] * 768,
            metadata=DocumentMetadata(
                source="government",
                upload_date=datetime.utcnow(),
                uploaded_by="admin1",
                version=1,
                state="Maharashtra",
                applicable_crops=["all"],
                tags=["central", "income_support", "financial"]
            ),
            is_active=True,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        ),
        Document(
            document_id="doc2",
            title="Wheat Cultivation Guidelines",
            category="guidelines",
            content="Guidelines for wheat cultivation in Punjab region.",
            content_language="en",
            embedding=[0.2] * 768,
            metadata=DocumentMetadata(
                source="agricultural_department",
                upload_date=datetime.utcnow(),
                uploaded_by="admin2",
                version=1,
                state="Punjab",
                applicable_crops=["wheat"],
                tags=["wheat", "cultivation", "punjab"]
            ),
            is_active=True,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        ),
        Document(
            document_id="doc3",
            title="PMFBY Crop Insurance",
            category="schemes",
            content="Pradhan Mantri Fasal Bima Yojana provides crop insurance.",
            content_language="en",
            embedding=[0.3] * 768,
            metadata=DocumentMetadata(
                source="government",
                upload_date=datetime.utcnow(),
                uploaded_by="admin1",
                version=1,
                state="All",
                applicable_crops=["paddy", "wheat", "cotton"],
                tags=["insurance", "crops", "risk_management"]
            ),
            is_active=True,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        ),
    ]


@pytest.fixture
def sample_search_queries():
    """Create sample search queries for testing."""
    return [
        "government schemes for farmers",
        "crop insurance information",
        "wheat cultivation guidelines",
        "PM-Kisan payment status",
        "fertilizer subsidy schemes",
    ]