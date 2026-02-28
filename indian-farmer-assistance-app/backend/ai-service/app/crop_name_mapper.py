"""
Crop name mapping utility to handle differences between crop recommendation 
and fertilizer recommendation datasets.
"""

# Mapping from crop recommendation dataset to fertilizer recommendation dataset
CROP_NAME_MAPPING = {
    # Exact matches (no mapping needed)
    'Sugarcane': 'Sugarcane',
    'Cotton': 'Cotton',
    'Potato': 'Potato',
    'Maize': 'Maize',
    'Rice': 'Rice',
    'Wheat': 'Wheat',
    
    # Vegetables and other crops mapped to closest fertilizer crop
    'Brinjal': 'Potato',  # Similar vegetable
    'Turmeric(raw)': 'Maize',  # Similar nutrient requirements
    'Mango(Raw-Ripe)': 'Cotton',  # Perennial crop
    'Onion': 'Potato',  # Similar vegetable
    'Arhar Dal(Tur Dal)': 'Maize',  # Legume, similar to maize
    'Paddy(Basmati)': 'Rice',  # Rice variant
    'Grapes': 'Cotton',  # Perennial crop
    'Pumpkin': 'Potato',  # Vegetable
    'Coconut': 'Cotton',  # Perennial crop
    'Soyabean': 'Maize',  # Legume
    'Banana': 'Cotton',  # Perennial crop
    'Green Chilli': 'Potato',  # Vegetable
    'Mustard': 'Wheat',  # Rabi crop
    'Bhindi(Ladies Finger)': 'Potato',  # Vegetable
    'Cabbage': 'Potato',  # Vegetable
    'Bengal Gram(Gram)(Whole)': 'Wheat',  # Legume, rabi crop
    'Green Gram Dal(Moong Dal)': 'Maize',  # Legume
    'Cauliflower': 'Potato',  # Vegetable
}

def map_crop_name(crop_name: str) -> str:
    """
    Map a crop name from the recommendation dataset to the fertilizer dataset.
    
    Args:
        crop_name: The crop name from crop recommendation model
        
    Returns:
        The mapped crop name for fertilizer recommendation model
    """
    return CROP_NAME_MAPPING.get(crop_name, 'Maize')  # Default to Maize if not found
