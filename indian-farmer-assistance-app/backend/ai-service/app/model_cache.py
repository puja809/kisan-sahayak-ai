"""Model caching and versioning utilities for disease detection."""
import hashlib
import os
from datetime import datetime
from pathlib import Path
from typing import Dict, Optional

from app.logging_config import logger


class ModelCache:
    """Manages model caching and versioning."""

    def __init__(self, cache_dir: str = "/tmp/model_cache"):
        """
        Initialize model cache.
        
        Args:
            cache_dir: Directory to store cached models
        """
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        self.model_metadata: Dict[str, dict] = {}

    def get_cache_path(self, model_name: str, version: str) -> Path:
        """
        Get cache path for a model version.
        
        Args:
            model_name: Name of the model
            version: Version string
            
        Returns:
            Path to cached model
        """
        return self.cache_dir / f"{model_name}_v{version}"

    def is_cached(self, model_name: str, version: str) -> bool:
        """
        Check if model version is cached.
        
        Args:
            model_name: Name of the model
            version: Version string
            
        Returns:
            True if model is cached
        """
        cache_path = self.get_cache_path(model_name, version)
        return cache_path.exists()

    def get_cache_info(self, model_name: str, version: str) -> Optional[dict]:
        """
        Get metadata about cached model.
        
        Args:
            model_name: Name of the model
            version: Version string
            
        Returns:
            Cache metadata or None
        """
        cache_key = f"{model_name}:{version}"
        return self.model_metadata.get(cache_key)

    def set_cache_info(
        self,
        model_name: str,
        version: str,
        size_mb: float,
        checksum: str
    ) -> None:
        """
        Store metadata about cached model.
        
        Args:
            model_name: Name of the model
            version: Version string
            size_mb: Size of model in MB
            checksum: Checksum of model file
        """
        cache_key = f"{model_name}:{version}"
        self.model_metadata[cache_key] = {
            "model_name": model_name,
            "version": version,
            "size_mb": size_mb,
            "checksum": checksum,
            "cached_at": datetime.utcnow().isoformat(),
            "cache_path": str(self.get_cache_path(model_name, version))
        }
        logger.info(f"Cached model {model_name} v{version}: {size_mb:.2f}MB")

    def calculate_checksum(self, file_path: Path) -> str:
        """
        Calculate SHA256 checksum of file.
        
        Args:
            file_path: Path to file
            
        Returns:
            Hex checksum
        """
        sha256_hash = hashlib.sha256()
        with open(file_path, "rb") as f:
            for byte_block in iter(lambda: f.read(4096), b""):
                sha256_hash.update(byte_block)
        return sha256_hash.hexdigest()

    def get_cache_size_mb(self) -> float:
        """
        Get total size of cached models.
        
        Returns:
            Total size in MB
        """
        total_size = 0
        for item in self.cache_dir.rglob("*"):
            if item.is_file():
                total_size += item.stat().st_size
        return total_size / (1024 * 1024)

    def clear_cache(self) -> None:
        """Clear all cached models."""
        import shutil
        if self.cache_dir.exists():
            shutil.rmtree(self.cache_dir)
            self.cache_dir.mkdir(parents=True, exist_ok=True)
            self.model_metadata.clear()
            logger.info("Model cache cleared")


# Global model cache instance
model_cache = ModelCache()
