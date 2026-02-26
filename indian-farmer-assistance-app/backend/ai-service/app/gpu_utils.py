"""GPU utilities for disease detection model inference."""
import torch
from typing import Tuple

from app.logging_config import logger


class GPUManager:
    """Manages GPU availability and device selection."""

    @staticmethod
    def is_gpu_available() -> bool:
        """
        Check if GPU is available.
        
        Returns:
            True if CUDA GPU is available
        """
        return torch.cuda.is_available()

    @staticmethod
    def get_device() -> torch.device:
        """
        Get appropriate device for inference.
        
        Returns:
            torch.device (cuda if available, else cpu)
        """
        if torch.cuda.is_available():
            device = torch.device("cuda")
            logger.info(f"Using GPU: {torch.cuda.get_device_name(0)}")
            return device
        else:
            logger.info("GPU not available, using CPU")
            return torch.device("cpu")

    @staticmethod
    def get_gpu_memory_info() -> Tuple[float, float]:
        """
        Get GPU memory information.
        
        Returns:
            Tuple of (used_mb, total_mb)
        """
        if not torch.cuda.is_available():
            return 0.0, 0.0
        
        used = torch.cuda.memory_allocated() / 1024 / 1024
        total = torch.cuda.get_device_properties(0).total_memory / 1024 / 1024
        return used, total

    @staticmethod
    def clear_gpu_cache() -> None:
        """Clear GPU cache to free memory."""
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
            logger.info("GPU cache cleared")

    @staticmethod
    def log_gpu_stats() -> None:
        """Log GPU statistics."""
        if torch.cuda.is_available():
            used, total = GPUManager.get_gpu_memory_info()
            logger.info(f"GPU Memory: {used:.2f}MB / {total:.2f}MB")
        else:
            logger.info("GPU not available")


# Global GPU manager instance
gpu_manager = GPUManager()
