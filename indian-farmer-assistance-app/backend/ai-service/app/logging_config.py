"""Logging configuration for AI Service."""
import logging
import sys
from typing import Optional

from app.config import settings


def setup_logging(log_level: Optional[str] = None) -> logging.Logger:
    """Configure application logging."""
    level = log_level or settings.log_level
    numeric_level = getattr(logging, level.upper(), logging.INFO)

    # Create logger
    logger = logging.getLogger("ai_service")
    logger.setLevel(numeric_level)

    # Create console handler
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(numeric_level)

    # Create formatter
    formatter = logging.Formatter(
        fmt="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S"
    )
    handler.setFormatter(formatter)

    # Add handler to logger if not already present
    if not logger.handlers:
        logger.addHandler(handler)

    return logger


logger = setup_logging()