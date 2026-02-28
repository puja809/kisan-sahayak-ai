import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder
import joblib
import os

class CropRotationModel:
    def __init__(self):
        self.model = None
        self.label_encoders = {}
        self.feature_names = ['soil_pH', 'temperature', 'humidity', 'rainfall']
        self.categorical_features = ['previous_crop', 'soil_type', 'season']
        self.target_name = 'recommended_next_crop'
        
    def train(self, csv_path):
        """Train crop rotation model from CSV"""
        df = pd.read_csv(csv_path)
        
        X = df[self.feature_names + self.categorical_features].copy()
        y = df[self.target_name].copy()
        
        # Encode categorical features
        for col in self.categorical_features:
            self.label_encoders[col] = LabelEncoder()
            X[col] = self.label_encoders[col].fit_transform(X[col])
        
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
    
    def predict(self, previous_crop, soil_pH, soil_type, temperature, humidity, rainfall, season):
        """Predict next crop for rotation"""
        if self.model is None:
            raise ValueError("Model not trained. Call train() first.")
        
        # Encode categorical inputs
        prev_crop_encoded = self.label_encoders['previous_crop'].transform([previous_crop])[0]
        soil_type_encoded = self.label_encoders['soil_type'].transform([soil_type])[0]
        season_encoded = self.label_encoders['season'].transform([season])[0]
        
        X = np.array([[soil_pH, temperature, humidity, rainfall, 
                       prev_crop_encoded, soil_type_encoded, season_encoded]])
        
        pred_encoded = self.model.predict(X)[0]
        pred_proba = self.model.predict_proba(X)[0]
        
        next_crop = self.label_encoders['crop'].inverse_transform([pred_encoded])[0]
        confidence = float(np.max(pred_proba))
        
        return {
            'recommended_next_crop': next_crop,
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
            'feature_names': self.feature_names,
            'categorical_features': self.categorical_features
        }, model_path)
    
    def load(self, model_path):
        """Load trained model"""
        data = joblib.load(model_path)
        self.model = data['model']
        self.label_encoders = data['label_encoders']
        self.feature_names = data['feature_names']
        self.categorical_features = data['categorical_features']
        return self
