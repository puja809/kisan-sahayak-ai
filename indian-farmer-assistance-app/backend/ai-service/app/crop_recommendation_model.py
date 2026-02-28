import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder
import joblib
import os

class CropRecommendationModel:
    def __init__(self):
        self.model = None
        self.label_encoders = {}
        self.feature_names = ['N', 'P', 'K', 'temperature', 'humidity', 'pH', 'rainfall']
        self.target_name = 'crop'
        
    def train(self, csv_path):
        """Train crop recommendation model from CSV"""
        df = pd.read_csv(csv_path)
        
        # Encode categorical features
        X = df[self.feature_names].copy()
        y = df[self.target_name].copy()
        
        # Encode target
        self.label_encoders['crop'] = LabelEncoder()
        y_encoded = self.label_encoders['crop'].fit_transform(y)
        
        # Train model
        self.model = RandomForestClassifier(
            n_estimators=100,
            max_depth=15,
            random_state=42,
            n_jobs=-1
        )
        self.model.fit(X, y_encoded)
        
        return self
    
    def predict(self, N, P, K, temperature, humidity, pH, rainfall):
        """Predict crop given soil and weather parameters"""
        if self.model is None:
            raise ValueError("Model not trained. Call train() first.")
        
        X = np.array([[N, P, K, temperature, humidity, pH, rainfall]])
        pred_encoded = self.model.predict(X)[0]
        pred_proba = self.model.predict_proba(X)[0]
        
        crop = self.label_encoders['crop'].inverse_transform([pred_encoded])[0]
        confidence = float(np.max(pred_proba))
        
        return {
            'crop': crop,
            'confidence': confidence,
            'probabilities': {
                self.label_encoders['crop'].inverse_transform([i])[0]: float(p)
                for i, p in enumerate(pred_proba)
            }
        }
    
    def save(self, model_path):
        """Save trained model"""
        os.makedirs(os.path.dirname(model_path), exist_ok=True)
        joblib.dump({
            'model': self.model,
            'label_encoders': self.label_encoders,
            'feature_names': self.feature_names
        }, model_path)
    
    def load(self, model_path):
        """Load trained model"""
        data = joblib.load(model_path)
        self.model = data['model']
        self.label_encoders = data['label_encoders']
        self.feature_names = data['feature_names']
        return self
